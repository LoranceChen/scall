//import lorance.scall.{Auth, ContextCmdFlow, Password, Shell}
//import lorance.scall.Implicit._
///**
//  *
//  */
//object Example extends App {
//  //connect a local ssh
//  val localShell = Shell(Auth("localhost", "username", 22, Password("xxxxxx")))
//
//  //simple command
//  val pwd = localShell.exc("pwd")
//  println("pwd - " + pwd)
//
//  assert(pwd.isRight)
//
//  //a error cmd
//  val pwd1 = localShell.exc("pwd1")
//  println("pwd1 - " + pwd1) // Left full error code
//  assert(pwd1.isLeft)
//
//  //test stateful commands
//  val cdTmp = localShell.exc("cd ~/tmp")
//  val touchFile = localShell.exc("touch tmpfile")
//  println("current pwd - " + localShell.exc("pwd"))
//  val ls = localShell.exc("ls")
//  println("ls - " + ls)
//
//  //advanced
//  // 1. login another shell based on current shell which use sshpass -p 'pwd' ssh host@addr
//  val remoteShell = localShell.newShell(Auth("192.168.1.149", "username", 22, Password("xxxxxx")))
//
//  remoteShell match {
//    case Left(code) =>
//      println("connect fail, error code - " + code)
//    case Right(newShell) =>
//      //show remote pwd
//      val remotePwd = newShell.exc("pwd")
//      println("remote pwd - " + remotePwd)
//
//    /**
//      * do NOT access localShell if connected to remoteShell
//      * e.g localShell.exc("ls") is not allowed
//      *
//      * exit current Shell if you want access parent shell
//      */
//
//      //exit remote shell
//      newShell.exit()
//
//      val localPwd = localShell.exc("pwd")
//      println("localPwd" + localPwd)
//  }
//
//  //excBatch
//  println("============================================================")
//  println("===================execute batch command====================")
//  println("============================================================")
//  val rst = localShell.excBatch(
//    "cd /var",
//    "ls",
//    "lll".exclude, //ignore fail if fail
//    "pwd",
//    (former: Option[String]) => s"cd ${former.get}", // former result influence current command.
//    "pwd",
//    "lll", //throw exception
//    "echo 'heheda'"
//  )
//
//  rst.foreach(println)
//
//  //close ssh connection
//  localShell.disconnect()
//}
