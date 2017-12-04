package lorance.scall

import org.junit.Test


/**
  *
  */
class TerminalTest {

  @Test
  def cmd(): Unit = {
    val currentDir = System.getProperty("user.dir")
    println("current dir - " + currentDir)

    val terminal = new Terminal(Auth("localhost", "lorancechen", 22, Password(" ")))
    val ls = terminal.exc(Cmd("lsss"))(HostLevel(0))
    println("lsss - " + ls)

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", "xxx", 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)
    val ls2 = terminal.exc(Cmd("ls"))(HostLevel(1))
    println("ls - " + ls2)

    val exit = terminal.exit(HostLevel(1))
    println("exit - " + exit)

    val ls3 = terminal.exc(Cmd("ls"))(HostLevel(1))
    println("ls - " + ls3)

    terminal.disconnect()

    Thread.currentThread().join()
  }

  @Test
  def disconnectHasCmdRunning(): Unit = {
    val terminal = new Terminal(Auth("localhost", "lorancechen", 22, Password(" ")))

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", "xxx", 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)

    terminal.exc(Cmd("cd ./test"))(HostLevel(1))
    val loop = terminal.exc(Cmd("./loop.sh"))(HostLevel(1))
    println("loop - " + newShell)

  }

  @Test
  def disconnectNotCmdRunning(): Unit = {
    val terminal = new Terminal(Auth("localhost", "lorancechen", 22, Password(" ")))

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", "xxx", 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)

    val pwd = terminal.exc(Cmd("pwd"))(HostLevel(1))
    println("pwd - " + pwd)

    //disconnect network by hand


    Thread.currentThread().join()
  }
}
