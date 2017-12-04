//package lorance.scall
//
//import org.slf4j.LoggerFactory
//
//import scala.annotation.tailrec
//import scala.concurrent.{Future, Promise}
//import scala.concurrent.ExecutionContext.Implicits.global
//
//trait Key
//case class Password(value: String) extends Key
//case class IdentityFile(path: String) extends Key
//case class NonKey() extends Key
//
//case class Auth(host: String, name: String, port: Int, key: Key)
//
//case class Cmd(content: String)
//
///**
//  * append a flag for cmd
//  * @param cmd
//  */
//case class CmdFlow(cmd: Cmd, isExclude: Boolean = false) {
//  def exclude = CmdFlow(cmd, true)
//}
//
//case class ContextCmdFlow(f: Option[String] => CmdFlow) extends (Option[String] => CmdFlow) {
//  override def apply(v1: Option[String]) = f(v1)
//}
//
//class MarkCmd(override val content: String) extends Cmd(content)
//
//object Status extends Enumeration {
//  val BackEnd = Value(1, "BackEnd")
//  val Using = Value(2, "Using")
//  val Dropped = Value(3, "Dropped")
//}
//
//case class Error(code: Int, msg: String)
//class ShellLock //Shell public method should be mutex
//
//case class Config(connectTimeout: Int, //second
//                  serverAliveInterval: Int, //half test value
//                  serverAliveCountMax: Int //recommend this value > 1, 确保最少的等待时间
//                 )
///**
//  * with shell streaming, there should define a unique echo string to warp response information to distinct other info from response stream,
//  * that means, e.g. If I want execute `pwd`, so I should send a command string with `echo uniqueStr && pwd && echo uniqueStr || echo uniqueStr`
//  */
//case class Shell( auth: Auth,
//                  parent: Option[Shell] = None//a unique id string for identity the shell
//           )(implicit lock: ShellLock = new ShellLock, config: Config = Config(10, 3, 2)) {
//  val levelId: Int = parent.map(_.levelId + 1).getOrElse(0)
//
//  private implicit val logger = LoggerFactory.getLogger(this.getClass)
//  private var status = Status.Using //is the shell under using
//  private val jsch: JSchWrapper = parent.map(_.jsch).getOrElse(new JSchWrapper(auth, config))
//
//  private val echoBegin = s"echo '';echo '$SPLIT_BEGIN_Str'"
//  private val echoEnd = s"echo $$?;echo '$SPLIT_END_Str';echo '$levelId'"
//  private val echoSSH = s"""$echoBegin;echo $$?;echo "$SPLIT_END_Str";echo "${levelId + 1}""""
//
//  private val regex = defStr2UTF8("""(?s)(.*)\n(.*)""").r
//  private val onlyDigitsRegex = defStr2UTF8("^(\\d+)$").r
//
//  //return: connecting Shell when current Shell is disconnect
//  private val disconnectFur = Promise[Option[Shell]]
//  //event
//  val onDisconnect: Future[Option[Shell]] = {
//    parent match {
//      case None =>
//        jsch.onDisconnect.map(_ => None)
//      case Some(p) =>
//        disconnectFur.future
//    }
//  }
//
//  val init = {
//    //setting charset
//    var utf8CheckSuccess = false
//
//    //check support either en_US.UTF-8 or en_US.utf8
//    val checker_UTF8 = exc(Cmd("""locale -a | grep "en_US\.UTF\-8""""))
//    logger.debug("checker_UTF8: " + checker_UTF8)
//    checker_UTF8 match {
//      case Right(str) if str.matches("""en_US\.UTF\-8""") =>
//        exc(Cmd(defStr2UTF8("export LC_ALL=en_US.UTF-8")))
//        utf8CheckSuccess = true
//      case _ =>
//        val checker_utf8 = exc(Cmd(defStr2UTF8("""locale -a | grep "en_US\.utf8"""")))
//        checker_utf8 match {
//          case Right(str) if str.matches(defStr2UTF8("""en_US\.utf8""")) =>
//            exc(Cmd(defStr2UTF8("export LC_ALL=en_US.utf8")))
//            logger.debug("checker_utf8: " + checker_UTF8)
//
//            utf8CheckSuccess = true
//        }
//    }
//
//    if (!utf8CheckSuccess) {
//      this.disconnect()
//      throw ShellSettingLangException
//    }
//  }
//
//  /**
//    * a command could result with a string and
//    * @param cmd
//    * @return
//    *
//    * NOTIC: refer regex: (?s) https://stackoverflow.com/questions/45625134/scala-regex-cant-match-r-n-in-a-giving-string-which-contains-multiple-r-n/45625835#45625835
//    */
//  def exc(cmd: Cmd): Either[Error, String] = lock.synchronized {
//    if(status != Status.Using) {
//      throw ShellContextException(s"current status is $status")
//    }
//
//    val newCmd = echoCmdStr(cmd.content)
//    val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(newCmd)
//
//    checkConnect(rstLevelId, levelId)
//
//    val errorMsg = jsch.scallErrorStream.flashErrorMsg
//    if(code == 0) Right(result) else Left(Error(code, errorMsg))
//  }
//
//  /**
//    * new shell create when user context is change.Contains: 1. remote ssh 2. su - new user
//    * @param auth
//    * @return
//    */
//  def newShell(auth: Auth): Either[Error, Shell] = lock.synchronized {
//    if(status != Status.Using) {
//      throw ShellContextException(s"current status is $status")
//    }
//
//    val cmd = auth.key match {
//      case Password(password) =>
//        echoCmdStr(s"sshpass -p '$password' ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
//      case IdentityFile(filePath) =>
//        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -i '$filePath' -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
//      case NonKey() =>
//        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
//    }
//
////    jsch.scallInputStream.setCommandNoRsp(cmd)
//
////    val echoCmd = defStr2UTF8(echoEnd)
////    val ProtoData(split, rstLevelId) = jsch.scallInputStream.setCommandMultiTimes(echoCmd)
//
//
//    val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(cmd)
//
//    checkConnect(rstLevelId, levelId)
//
//    val errorMsg = jsch.scallErrorStream.flashErrorMsg
//
//    if(code == 0) {
//      status = Status.BackEnd
//      Right(Shell(auth, Some(this))(this.lock))
//    } else
//      Left(Error(code, errorMsg))
//  }
//
//  /**
//    * exit current shell
//    * @return
//    */
//  def exit(): Either[Error, Shell] = lock.synchronized {
//    if(status != Status.Using) {
//      throw ShellContextException(s"current status is $status")
//    }
//
//    if(parent.isDefined) {
//      val cmd = echoCmdStr("exit")
//      val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(cmd)
//
//      val errorMsg = jsch.scallErrorStream.flashErrorMsg
//
//      val curParent = parent.get
//      val rst = if (code == 0) {
//        status = Status.Dropped
//        curParent.status = Status.Using
//        Right(curParent)
//      } else
//        Left(Error(code, errorMsg))
//
//      disconnectFur.trySuccess(parent)
//      rst
//    } else {
//      throw ExitRootShell
//    }
//  }
//
//  /**
//    * NOTICE: Stream result will not be evolute immediate
//    * @param cmds: former command result => the Cmd
//    * @return
//    */
//  def excBatch(cmds: ContextCmdFlow*): Stream[Either[Error, String]] = lock.synchronized {
//    var formerResult: Option[String] = None
//    var index = -1
//    cmds.toStream.map{cmdFlow =>
//      index += 1
//      val cmdFlowRst = cmdFlow(formerResult)
//      val cmdResult = exc(cmdFlowRst.cmd)
//      cmdResult match {
//        case right @ Right(load) =>
//          formerResult = Some(load)
//          right
//        case left @ Left(error) =>
//          if(cmdFlowRst.isExclude) {
//            formerResult = None
//            left
//          }
//          else throw ExcBatchFailException(error, cmdFlowRst.cmd, index)
//      }
//    }
//  }
//
//  def disconnect(): Unit = lock.synchronized {
//    jsch.close()
//  }
//
//  /**
//    * append split to command string
//    */
//  private def echoCmdStr(cmd: String) = {
//    defStr2UTF8(s"$echoBegin;$cmd;$echoEnd")
//  }
//
//  private def checkConnect(curLevelId: Int, aimLevelId: Int) = {
////    println(s"checkConnect($curLevelId: Int, $aimLevelId: Int)")
//    if(curLevelId != aimLevelId) {
//      // find current connecting shell
//      @tailrec def findTheShell(curShell: Shell): Shell = {
//        if(parent.get.levelId == aimLevelId) parent.get
//        else findTheShell(parent.get)
//      }
//
//      disconnectFur.trySuccess(Some(findTheShell(this)))
//    }
//  }
//}
