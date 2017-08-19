package lorance.scall

case class Auth(host: String, name: String, port: Int, password: String)

object Status extends Enumeration {
  val BackEnd = Value(1, "BackEnd")
  val Using = Value(2, "Using")
  val Dropped = Value(3, "Dropped")
}

/**
  * with shell streaming, there should define a unique echo string to warp response information to distinct other info from response stream,
  * that means, e.g. If I want execute `pwd`, so I should send a command string with `echo uniqueStr && pwd && echo uniqueStr || echo uniqueStr`
  *
  */
case class Shell(auth: Auth,
             parent: Option[Shell] = None
           ) {
  private var status = Status.Using //is the shell under using
  val jsch: JSchWrapper = parent.map(_.jsch).getOrElse(new JSchWrapper(auth))

  /**
    * a command could result with a string and
    * @param cmd
    * @return
    *
    * NOTIC: refer regex: (?s) https://stackoverflow.com/questions/45625134/scala-regex-cant-match-r-n-in-a-giving-string-which-contains-multiple-r-n/45625835#45625835
    */
  private val regex = """(?s)(.*)\r\n(.*)""".r
  def exc(cmd: String): Either[Int, String] = {
    assert(status == Status.Using, s"current status is $status")

    val newCmd = s"echo $SPLIT && " + cmd + s" && echo $$? || echo $$? && echo $SPLIT || echo $SPLIT"
    val splited = jsch.scallInputStream.setCommand(newCmd)
    val onlyDigitsRegex = "^(\\d+)$".r
    val (result, code) = splited match {
      case onlyDigitsRegex(cde)  =>
        ("", cde.toInt)
      case regex(rst, cde) =>
        (rst, cde.toInt)
    }

    if(code == 0) Right(result) else Left(code)
  }

  private val newRegex = """(?s).*\r\n(.*)""".r
  def newShell(auth: Auth): Either[Int, Shell] = {
    assert(status == Status.Using, s"current status is $status")

    val cmd = s"echo $SPLIT && TERM=dumb sshpass -p '${auth.password}' ssh -t -o StrictHostKeyChecking=no ${auth.name}@${auth.host} -p ${auth.port}"
    jsch.scallInputStream.setCommandNoRsp(cmd)

    val echoCmd = s"echo $$? || echo $$? && echo $SPLIT || echo $SPLIT"
    val splited = jsch.scallInputStream.setCommand(echoCmd)

    val code = splited match {
      case newRegex(cde) =>
        cde.toInt
    }

    if(code == 0) {
      status = Status.BackEnd
      Right(Shell(auth, Some(this)))
    } else
      Left(code)
  }

  /**
    * exit current shell
    * @return
    *
    */
  def exit(): Either[Int, Shell] = {
    assert(status == Status.Using, s"current status is $status")
    if(parent.isDefined) {
      val cmd = s"echo $SPLIT && exit"
      jsch.scallInputStream.setCommandNoRsp(cmd)

      //todo wait response when send exit cmd
      Thread.sleep(1000)
      val echoCmd = s"echo $$? || echo $$? && echo $SPLIT || echo $SPLIT"
      val splited = jsch.scallInputStream.setCommand(echoCmd)

      val code = splited match {
        case newRegex(cde) =>
          cde.toInt
      }

      if (code == 0) {
        status = Status.Dropped
        parent.get.status = Status.Using
        Right(parent.get)
      } else
        Left(code)
    } else {
      throw new Exception("root Shell env can't exit, use disconnect if you want close")
    }
  }

  def disconnect() = {
    jsch.close()
  }
}
