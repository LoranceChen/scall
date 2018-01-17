package lorance.scall

import org.junit.Test

class SessionTerminalTest {

  @Test
  def simpleUsage(): Unit = {
    val sessionTerminal = new SessionTerminal(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2))

    println(sessionTerminal.exc(Cmd("""cd ./not-exist-dir && pwd""")))
    println(sessionTerminal.exc(Cmd("""pwd""")))
  }

}
