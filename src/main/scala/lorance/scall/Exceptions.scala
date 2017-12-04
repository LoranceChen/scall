package lorance.scall

/**
  * scall Exceptions
  */
class ScallException(msg: String) extends RuntimeException(msg)

case class ExcFailException(error: Error) extends ScallException(error.toString)
//case class ExcBatchFailException(error: Error, cmd: Cmd, index: Int) extends
//  ScallException(s"${index}st command: $cmd, execute fail: $error")
//
//case object ExitRootShell extends ScallException("root Shell env can't exit, use disconnect if you want close")
case class StreamClosedException(msg: String) extends ScallException(msg)
//case class ShellContextException(msg: String) extends ScallException(msg)
//case class NetworkDisconnect(shell: Shell) extends ScallException(s"Shell back to level:${shell.levelId}")
//case object ShellLevelFlow extends ScallException("can NOT setting sub shell more then 256")


//Terminal exception
class TerminalException(msg: String) extends RuntimeException(msg)
case class TerminalSettingLangException(errMsg: String, curHostLevel: Int) extends ScallException(s"can NOT setting LANG to en_US.UTF-8, errMsg=$errMsg; curHostLevel=$curHostLevel")
case class TerminalDisconnectException(errMsg: String, load: String, curHostLevel: Int) extends TerminalException(msg = s"errMsg=$errMsg;load=$load;curHostLevel=$curHostLevel" )
