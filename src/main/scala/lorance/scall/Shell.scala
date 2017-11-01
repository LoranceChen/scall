package lorance.scall

trait Key
case class Password(value: String) extends Key
case class IdentityFile(path: String) extends Key
case class NonKey() extends Key

case class Auth(host: String, name: String, port: Int, key: Key)

case class Cmd(content: String)

/**
  * append a flag for cmd
  * @param cmd
  */
case class CmdFlow(cmd: Cmd, isExclude: Boolean = false) {
  def exclude = CmdFlow(cmd, true)
}

case class ContextCmdFlow(f: Option[String] => CmdFlow) extends (Option[String] => CmdFlow) {
  override def apply(v1: Option[String]) = f(v1)
}

class MarkCmd(override val content: String) extends Cmd(content)

object Status extends Enumeration {
  val BackEnd = Value(1, "BackEnd")
  val Using = Value(2, "Using")
  val Dropped = Value(3, "Dropped")
}

case class Error(code: Int, msg: String)
class ShellLock //Shell public method should be mutex

case class Config(connectTimeout: Int //second
                 )
/**
  * with shell streaming, there should define a unique echo string to warp response information to distinct other info from response stream,
  * that means, e.g. If I want execute `pwd`, so I should send a command string with `echo uniqueStr && pwd && echo uniqueStr || echo uniqueStr`
  */
case class Shell(auth: Auth,
             parent: Option[Shell] = None
           )(implicit lock: ShellLock = new ShellLock, config: Config = Config(10)) {
  private var status = Status.Using //is the shell under using
  val jsch: JSchWrapper = parent.map(_.jsch).getOrElse(new JSchWrapper(auth, config))

  private val regex = """(?s)(.*)\n(.*)""".r
  private val onlyDigitsRegex = "^(\\d+)$".r

  val init = {
    //setting charset
    exc(Cmd("LANG=en_US.UTF-8"))
  }

  /**
    * a command could result with a string and
    * @param cmd
    * @return
    *
    * NOTIC: refer regex: (?s) https://stackoverflow.com/questions/45625134/scala-regex-cant-match-r-n-in-a-giving-string-which-contains-multiple-r-n/45625835#45625835
    */
//  def exc(cmd: Cmd): Either[Error, String] = lock.synchronized {
//    assert(status == Status.Using, s"current status is $status")
//
//    val newCmd = s"echo '' && echo $SPLIT_BEGIN && " + cmd.content + s" && echo $$? || echo $$? && echo $SPLIT_END || echo $SPLIT_END"
//    val split = jsch.scallInputStream.setCommand(newCmd)
//    val (result, code) = split match {
//      case onlyDigitsRegex(cde)  =>
//        ("", cde.toInt)
//      case regex(rst, cde) =>
//        (rst, cde.toInt)
//    }
//
//    val errorMsg = jsch.scallErrorStream.flashErrorMsg
//    if(code == 0) Right(result) else Left(Error(code, errorMsg))
//  }

  def exc(cmd: Cmd): Either[Error, String] = lock.synchronized {
    if(status != Status.Using) {
      throw ShellContextException(s"current status is $status")
    }

    val newCmd = s"echo '' && echo $SPLIT_BEGIN && " + cmd.content
    jsch.scallInputStream.setCommandNoRsp(newCmd)

    val newCmd2 =  s"echo $$? || echo $$? && echo $SPLIT_END || echo $SPLIT_END"
    val split2 = jsch.scallInputStream.setCommand(newCmd2)

    val (result, code) = split2 match {
      case onlyDigitsRegex(cde)  =>
        ("", cde.toInt)
      case regex(rst, cde) =>
        (rst, cde.toInt)
    }

    val errorMsg = jsch.scallErrorStream.flashErrorMsg
    if(code == 0) Right(result) else Left(Error(code, errorMsg))
  }

//  def sudo(cmd: Cmd): Either[Error, String] = lock.synchronized {
//    excSplit(Cmd("sudo " + cmd.content))
//  }

//  private val newRegex = """(?s).*\n(.*)""".r
//  todo catch error such as network disconnect form new shell and notify Shell class to forbid current Shell and exit to parent automatic.
  //    use heartbeat at the library rather then use -o ServerAliveInterval=30
  def newShell(auth: Auth): Either[Error, Shell] = lock.synchronized {
    if(status != Status.Using) {
      throw ShellContextException(s"current status is $status")
    }

//    val cmd = s"echo $SPLIT && TERM=dumb sshpass -p '${auth.password}' ssh -t -o StrictHostKeyChecking=no ${auth.name}@${auth.host} -p ${auth.port}"
//    val cmd = s"echo '' && echo $SPLIT_BEGIN && sshpass -p '${auth.password}' ssh -T -o StrictHostKeyChecking=no ${auth.name}@${auth.host} -p ${auth.port}"
    val cmd = auth.key match {
      case Password(password) =>
        s"echo '' && echo $SPLIT_BEGIN && sshpass -p '$password' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} ${auth.name}@${auth.host} -p ${auth.port}"
      case IdentityFile(filePath) =>
        s"echo '' && echo $SPLIT_BEGIN && ssh -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} -i '$filePath' ${auth.name}@${auth.host} -p ${auth.port}"
      case NonKey() =>
        s"echo '' && echo $SPLIT_BEGIN && ssh -o StrictHostKeyChecking=no -o ConnectTimeout=${config.connectTimeout} ${auth.name}@${auth.host} -p ${auth.port}"
    }

    jsch.scallInputStream.setCommandNoRsp(cmd)

    val echoCmd = s"echo $$? || echo $$? && echo $SPLIT_END || echo $SPLIT_END"
    val split = jsch.scallInputStream.setCommand(echoCmd)

    val (_, code) = split match {
      case onlyDigitsRegex(cde) => //only print a `echo $?` after `exit`
        ("", cde.toInt)
      case regex(rst, cde) =>
        (rst, cde.toInt)
    }

    val errorMsg = jsch.scallErrorStream.flashErrorMsg

    if(code == 0) {
      status = Status.BackEnd
      Right(Shell(auth, Some(this))(this.lock))
    } else
      Left(Error(code, errorMsg))
  }

  /**
    * exit current shell
    * @return
    */
  def exit(): Either[Error, Shell] = lock.synchronized {
    if(status != Status.Using) {
      throw ShellContextException(s"current status is $status")
    }

    if(parent.isDefined) {
      val cmd = s"echo '' && echo $SPLIT_BEGIN && exit"
      jsch.scallInputStream.setCommandNoRsp(cmd)

      val echoCmd = s"echo $$? || echo $$? && echo $SPLIT_END || echo $SPLIT_END"
      val split = jsch.scallInputStream.setCommandMultiTimes(echoCmd)

      val (_, code) = split match {
        case onlyDigitsRegex(cde) => //exit success - only print a `echo $?` after `exit`
          ("", cde.toInt)
        case regex(rst, cde) => //maybe `exit` fail and print some message before execute `echo $?`
          (rst, cde.toInt)
      }

      val errorMsg = jsch.scallErrorStream.flashErrorMsg

      val curParent = parent.get
      val rst = if (code == 0) {
        status = Status.Dropped
        curParent.status = Status.Using
        Right(curParent)
      } else
        Left(Error(code, errorMsg))

      rst
    } else {
      throw ExitRootShell
    }
  }

  /**
    * NOTICE: Stream result will not be evolute immediate
    * @param cmds: former command result => the Cmd
    * @return
    */
  def excBatch(cmds: ContextCmdFlow*): Stream[Either[Error, String]] = lock.synchronized {
    var formerResult: Option[String] = None
    var index = -1
    cmds.toStream.map{cmdFlow =>
      index += 1
      val cmdFlowRst = cmdFlow(formerResult)
      val cmdResult = exc(cmdFlowRst.cmd)
      cmdResult match {
        case right @ Right(load) =>
          formerResult = Some(load)
          right
        case left @ Left(error) =>
          if(cmdFlowRst.isExclude) {
            formerResult = None
            left
          }
          else throw ExcBatchFailException(error, cmdFlowRst.cmd, index)
      }
    }
  }

  def disconnect(): Unit = lock.synchronized {
    jsch.close()
  }
}
