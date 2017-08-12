
/**
  * use (?s) match multiple lines
  */
object RegexTest extends App {
  val r = "(?s)(.*)\r\n(.*)".r
  val str = "abcd\r\nabc\r\nppp"

  str match {
    case r(a,b) =>
      val newa = a.replace("\r\n", "=r==n=")
      println((newa,b))
    case _ =>
      println("fail - ")
  }
}
