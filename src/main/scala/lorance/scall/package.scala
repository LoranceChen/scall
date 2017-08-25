package lorance

import scala.util.control.NonFatal

/**
  * scall Global constants and util method
  */
package object scall {
  val SPLIT_BEGIN = "SPLIT_BEGIN-8290892a-032a-4dcb-b59f-86000ac4db02"
  val SPLIT_END = "SPLIT_END-8990ab91-2722-4885-a39d-fef4abe68444"
  val MAGIC_SPLIT_BEGIN = s"\n$SPLIT_BEGIN\n"
  val MAGIC_SPLIT_END = s"\n$SPLIT_END\n"

  /**
    * a method must be right, if not
    * @param cmdResult is a Cmd executed result
    */
  def right(cmdResult: Either[Error, String]): String = {
    cmdResult match {
      case Right(rst) => rst
      case Left(error) =>
        throw ExcFailException(error)
    }
  }

  /**
    * print the fail message if executing has some error.
    * @param cmdResult is a Cmd executed result
    */
  def failPrint(cmdResult: Either[Error, String]): Unit = {
    cmdResult match {
      case Left(error) => println(error)
      case _ => Unit
    }
  }

  /**
    * print command result
    */
  //do some action by default
//  def doPrint(cmdResult: Either[Error, String])(implicit rst: String => String = x => x) = {
  def doPrint(cmdResult: Either[Error, String]) = {
    println(cmdResult)
    cmdResult
  }

  def loan[T](shell: Shell)(f: Shell => T) = {
    try {
      f(shell)
    } catch {
      case NonFatal(e) => throw e
    } finally {
      shell.disconnect()
    }

  }
}
