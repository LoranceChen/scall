package lorance.scall

/**
  * scall Exceptions
  */
class ScallException(msg: String) extends Exception(msg)

case class ExcFailException(error: Error) extends ScallException(error.toString)