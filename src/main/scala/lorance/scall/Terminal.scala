package lorance.scall

import org.slf4j.LoggerFactory
import rx.lang.scala.{Observable, Subject}
import scala.concurrent.ExecutionContext.Implicits.global

case class HostLevel(value: Int)

/**
  *
  */
class Terminal(auth: Auth) {
  implicit val config: Config = Config(10, 3, 2)

  var curLevelId = 0

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsch: JSchWrapper = new JSchWrapper(auth, config)

  private def echoBegin = s"echo '';echo '$SPLIT_BEGIN_Str'"
  private def echoEnd = s"echo $$?;echo '$SPLIT_END_Str';echo '$curLevelId'"
  private def echoSSH = s"""$echoBegin;echo $$?;echo "$SPLIT_END_Str";echo "${curLevelId + 1}""""

  case class DisconnectMsg(error: String, curHostLevel: Int)
  private val disconnectSub = Subject[DisconnectMsg]()

  //register disconnect event
  jsch.onDisconnect.map{_ =>
    //initial session disconnect
    logger.info("initial_ssh_closed")
    disconnectSub.onNext(DisconnectMsg("initial_ssh_closed", -1))
    disconnectSub.onCompleted()
  }

  jsch.scallOutputStream.noReplyObv.subscribe(protoData => {
    logger.warn(s"ssh_disconnected_to: ${protoData.hostLevel}")
    curLevelId = protoData.hostLevel
    disconnectSub.onNext(DisconnectMsg(protoData.data, protoData.hostLevel))
  })

  val disConnectStream: Observable[DisconnectMsg] = disconnectSub
  disConnectStream.subscribe() //make sure stream will emit

  def init(implicit expectHostLevelAfterCmd: HostLevel): Unit = {
    //setting charset
    var utf8CheckSuccess = false

    //check support either en_US.UTF-8 or en_US.utf8
    val checker_UTF8 = exc(Cmd("""locale -a | grep "en_US\.UTF\-8""""))
    logger.debug("checker_UTF8: " + checker_UTF8)
    checker_UTF8 match {
      case Right(str) if str.matches("""en_US\.UTF\-8""") =>
        exc(Cmd(defStr2UTF8("export LC_ALL=en_US.UTF-8")))
        utf8CheckSuccess = true
      case _ =>
        val checker_utf8 = exc(Cmd(defStr2UTF8("""locale -a | grep "en_US\.utf8"""")))
        checker_utf8 match {
          case Right(str) if str.matches(defStr2UTF8("""en_US\.utf8""")) =>
            exc(Cmd(defStr2UTF8("export LC_ALL=en_US.utf8")))
            logger.debug("checker_utf8: " + checker_UTF8)

            utf8CheckSuccess = true
        }
    }

    if (!utf8CheckSuccess) {
      this.exit(HostLevel(expectHostLevelAfterCmd.value - 1))
      throw TerminalSettingLangException("", curLevelId)
    }
  }

  init(HostLevel(0))

  /**
    * a command could result with a string and
    * @param cmd
    * @return
    *
    * NOTIC: refer regex: (?s) https://stackoverflow.com/questions/45625134/scala-regex-cant-match-r-n-in-a-giving-string-which-contains-multiple-r-n/45625835#45625835
    */
  def exc(cmd: Cmd)(implicit expectHostLevelAfterCmd: HostLevel): Either[Error, String] = {
    val newCmd = echoCmdStr(cmd.content)

    val parsedProto = jsch.scallInputStream.setCommand(newCmd)
    val ParsedProto(result, code, rstLevelId) = parsedProto
    val errorMsg = jsch.scallErrorStream.flashErrorMsg

    if(curLevelId != rstLevelId) {
      curLevelId = rstLevelId
      val ex = TerminalDisconnectException(errorMsg, result, rstLevelId)
      logger.error("check_host_level_fail: ", ex)
      throw ex
    } else if(expectHostLevelAfterCmd.value != rstLevelId) {
      val ex = TerminalHostLevelException(expectHostLevelAfterCmd.value, rstLevelId)
      logger.error("check_host_level_fail: ", ex)
      throw ex
    }
    curLevelId = rstLevelId

    if(code == 0) {
      Right(result)
    } else {
      //check disconnect
      Left(Error(code, errorMsg))
    }
  }

  def newShell(auth: Auth)(implicit expectHostLevelAfterCmd: HostLevel): Either[Error, String] = {

    val cmd = auth.key match {
      case Password(password) =>
        echoCmdStr(s"sshpass -p '$password' ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name.map(_ + "@").getOrElse("")}${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
      case IdentityFile(filePath) =>
        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -i '$filePath' -T ${auth.name.map(_ + "@").getOrElse("")}${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
      case NonKey =>
        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name.map(_ + "@").getOrElse("")}${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
    }

    val parsedProto = jsch.scallInputStream.setCommand(cmd)
    val ParsedProto(result, code, rstLevelId) = parsedProto
    val errorMsg = jsch.scallErrorStream.flashErrorMsg

    if((curLevelId + 1) != rstLevelId) {
      curLevelId = rstLevelId
      val ex = TerminalDisconnectException(errorMsg, result, rstLevelId)
      logger.error("check_host_level_fail: ", ex)
      throw ex
    } else if(expectHostLevelAfterCmd.value != rstLevelId) {
      val ex = TerminalHostLevelException(expectHostLevelAfterCmd.value, rstLevelId)
      logger.error("check_host_level_fail: ", ex)
      throw ex
    }
    curLevelId = rstLevelId

    if(code == 0) {
      init(expectHostLevelAfterCmd)
      Right(result)
    } else {
      //check disconnect
      Left(Error(code, errorMsg))
    }
  }

  def exit(implicit expectHostLevelAfterCmd: HostLevel): Either[Error, String] = {
    if(curLevelId <= 0) {
      disconnect()
      Right("")
    } else {
      val parsedProto = jsch.scallInputStream.setCommand(defStr2UTF8("exit"))
      val ParsedProto(result, code, rstLevelId) = parsedProto
      val errorMsg = jsch.scallErrorStream.flashErrorMsg

      if(curLevelId - 1 != rstLevelId) {
        curLevelId = rstLevelId
        val ex = TerminalDisconnectException(errorMsg, result, rstLevelId)
        logger.error("check_host_level_fail: ", ex)
        throw ex
      } else if(expectHostLevelAfterCmd.value != rstLevelId) {
        val ex = TerminalHostLevelException(expectHostLevelAfterCmd.value, rstLevelId)
        logger.error("check_host_level_fail: ", ex)
        throw ex
      }
      curLevelId = rstLevelId

      if (code == 0) Right(result) else {
        //check disconnect
        Left(Error(code, errorMsg))
      }
    }
  }

  def disconnect(): Unit = {
    jsch.close()
  }

  /**
    * append split to command string
    */
  private def echoCmdStr(cmd: String) = {
    defStr2UTF8(s"$echoBegin;$cmd;$echoEnd")
  }

}
