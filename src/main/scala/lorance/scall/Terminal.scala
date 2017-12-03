package lorance.scall

import org.slf4j.LoggerFactory

/**
  *
  */
class Terminal(auth: Auth) {
  implicit val config: Config = Config(10, 3, 2)

  implicit var curLevelId = 0

  private implicit val logger = LoggerFactory.getLogger(this.getClass)
  private val jsch: JSchWrapper = new JSchWrapper(auth, config)

  private def echoBegin = s"echo '';echo '$SPLIT_BEGIN_Str'"
  private def echoEnd = s"echo $$?;echo '$SPLIT_END_Str';echo '$curLevelId'"
  private def echoSSH = s"""$echoBegin;echo $$?;echo "$SPLIT_END_Str";echo "${curLevelId + 1}""""

  val init = {
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
      this.disconnect()
      throw ShellSettingLangException
    }
  }

  /**
    * a command could result with a string and
    * @param cmd
    * @return
    *
    * NOTIC: refer regex: (?s) https://stackoverflow.com/questions/45625134/scala-regex-cant-match-r-n-in-a-giving-string-which-contains-multiple-r-n/45625835#45625835
    */
  def exc(cmd: Cmd)(implicit hostLevel: Int = curLevelId): Either[Error, String] = {
    val newCmd = echoCmdStr(cmd.content)
    val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(newCmd)

    assert(hostLevel == rstLevelId)
    curLevelId = rstLevelId

    val errorMsg = jsch.scallErrorStream.flashErrorMsg
    if(code == 0) Right(result) else {
      //check disconnect
      Left(Error(code, errorMsg))
    }
  }

  def newShell(auth: Auth)(implicit hostLevel: Int = curLevelId + 1): Either[Error, String] = {

    val cmd = auth.key match {
      case Password(password) =>
        echoCmdStr(s"sshpass -p '$password' ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
      case IdentityFile(filePath) =>
        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -i '$filePath' -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
      case NonKey() =>
        echoCmdStr(s"ssh -o ServerAliveInterval=${config.serverAliveInterval} -o ServerAliveCountMax=${config.serverAliveCountMax} -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -T ${auth.name}@${auth.host} -p${auth.port} '$echoSSH;/bin/bash'")
    }

    val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(cmd)

    assert(rstLevelId == hostLevel)
    curLevelId = rstLevelId

    val errorMsg = jsch.scallErrorStream.flashErrorMsg

    if(code == 0) Right(result) else {
      //check disconnect
      Left(Error(code, errorMsg))
    }
  }

  def exit(implicit hostLevel: Int = curLevelId): Either[Error, String] = {
    assert(curLevelId > 0)

    val ParsedProto(result, code, rstLevelId) = jsch.scallInputStream.setCommand(defStr2UTF8("exit"))

    assert(rstLevelId == hostLevel)
    curLevelId = rstLevelId

    val errorMsg = jsch.scallErrorStream.flashErrorMsg

    if(code == 0) Right(result) else {
      //check disconnect
      Left(Error(code, errorMsg))
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
