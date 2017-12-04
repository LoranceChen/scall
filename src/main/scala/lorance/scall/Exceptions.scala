package lorance.scall

/**
  * scall Exceptions
  */
class ScallException(msg: String) extends RuntimeException(msg)

case class ExcFailException(error: Error) extends ScallException(error.toString)
case class StreamClosedException(msg: String) extends ScallException(msg)


//Terminal exception
class TerminalException(msg: String) extends RuntimeException(msg)
case class TerminalSettingLangException(errMsg: String, curHostLevel: Int) extends ScallException(s"can NOT setting LANG to en_US.UTF-8, errMsg=$errMsg; curHostLevel=$curHostLevel")
case class TerminalDisconnectException(errMsg: String, load: String, curHostLevel: Int) extends TerminalException(msg = s"errMsg=$errMsg;load=$load;curHostLevel=$curHostLevel" )
case class TerminalHostLevelException(expectLevel: Int, rstLevel: Int) extends TerminalException(msg = s"expect host level: $expectLevel, get host level: $rstLevel" )
