package lorance.scall

import org.junit.Test

class SessionTerminalTest {

  @Test
  def simpleUsage(): Unit = {
    val sessionTerminal = new SessionTerminal(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2))

    println(sessionTerminal.exc(Cmd("""cd ./not-exist-dir && pwd""")))
    println(sessionTerminal.exc(Cmd("""pwd""")))
  }

  @Test
  def simpleUsage2(): Unit = {
//    val sessionTerminal = new SessionTerminal2(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2))
//
//    println(sessionTerminal.exc(Cmd("""cd ./not-exist-dir && pwd""")))
//    println(sessionTerminal.exc(Cmd("""pwd""")))
  }

  @Test
  def simpleUsage3(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val sessionTerminal = new SessionTerminalPool(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2), 20)

    (1 to 100).foreach(_ =>
      new Thread(() => {
        sessionTerminal.excAsync(Cmd("""cd ./not-exist-dir && pwd""")).foreach(println)
        sessionTerminal.excAsync(Cmd("""cd ~/tmp;pwd""")).foreach(println)
      }).start())

    Thread.currentThread().join()
  }
}
