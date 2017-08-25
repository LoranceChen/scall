package lorance.scall

/**
  * scall Exceptions
  */
class ScallException(msg: String) extends Exception(msg)

case class ExcFailException(error: Error) extends ScallException(error.toString)
case class ExcBatchFailException(error: Error, cmd: Cmd, index: Int) extends
  ScallException(s"${index}st command: $cmd, execute fail: $error")

case object ExitRootShell extends ScallException("root Shell env can't exit, use disconnect if you want close")