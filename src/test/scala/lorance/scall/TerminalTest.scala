package lorance.scall

import org.junit.Test


/**
  *
  */
class TerminalTest {

  @Test
  def cmd(): Unit = {
    val terminal = new Terminal(Auth("localhost", Some("xxx"), 22, Password("xxx")))
    val lsss = terminal.exc(Cmd("lsss"))(HostLevel(0))
    println("lsss - " + lsss)

    val ls = terminal.exc(Cmd("ls"))(HostLevel(0))
    println("ls - " + ls)

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", Some("xxx"), 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)
    val ls2 = terminal.exc(Cmd("ls"))(HostLevel(1))
    println("ls - " + ls2)

    val exit = terminal.exit(HostLevel(0))
    println("exit - " + exit)

    val ls3 = terminal.exc(Cmd("ls"))(HostLevel(0))
    println("ls - " + ls3)

    terminal.disconnect()
  }

  @Test
  def disconnectHasCmdRunning(): Unit = {
    val terminal = new Terminal(Auth("localhost", Some("xxx"), 22, Password("xx")))

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", Some("xxx"), 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)

    terminal.exc(Cmd("cd ./test"))(HostLevel(1))
    val loop = terminal.exc(Cmd("./loop.sh"))(HostLevel(1))
    println("loop - " + newShell)

  }

  @Test
  def disconnectNotCmdRunning(): Unit = {
    val terminal = new Terminal(Auth("localhost", Some("xxx"), 22, Password("xx")))

    val newShell = terminal.newShell(Auth("xxx.xxx.xxx.xxx", Some("xxx"), 22, Password("xxxx")))(HostLevel(1))
    println("newShell - " + newShell)

    val pwd = terminal.exc(Cmd("pwd"))(HostLevel(1))
    println("pwd - " + pwd)

    //disconnect network by hand


    Thread.currentThread().join()
  }
}
