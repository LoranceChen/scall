package lorance.scall

import java.io.InputStream
import java.util.concurrent.Semaphore

import rx.lang.scala.Subject

/**
  * send cmd
  */
class ScallInputStream(outputStream: ScallOutputStream)
                      (implicit writeSemaphore: Semaphore, writeLock: WriteLock) extends InputStream {
  private var readied = false
  private var cmd: Array[Byte] = Array.emptyByteArray
  private var curIndex = 0
  private var closedMark = false
  private var curResult: String = _
  outputStream.outputObv.subscribe(curResult = _)

  val readCompleteObv = Subject[Unit]()
  val lockRead = new Object()

  //not support
  def setCommand(cmd: String) = {
    assert(!closedMark, "Input Stream has been closed")
    //acquire and release should under different thread.
    writeSemaphore.acquire()

    this.cmd = cmd.getBytes
    readied = true
//    println(s"setCommand - $cmd")

    //read data
    lockRead.synchronized(lockRead.notify())

    //wait data form OutputStream
    writeLock.synchronized(writeLock.wait())
    curResult
  }

  val noRspLock = new Object
  def setCommandNoRsp(cmd: String) = {
    assert(!closedMark, "Input Stream has been closed")

    //acquire and release should under different thread.
    writeSemaphore.acquire()

    this.cmd = cmd.getBytes
    readied = true

    //register must before notify read
    //only register once
    val subscription = readCompleteObv.first.subscribe(x => {
//      println(s"notify - $cmd")
      noRspLock.synchronized(noRspLock.notify())

    })

    //read data
    lockRead.synchronized(lockRead.notify())

//    println("wait")
    noRspLock.synchronized(noRspLock.wait())
//    println("wait return ")
    //todo return wait send completed
  }
//
//  def setCmd(cmd: String) = {
//    //acquire and release should under different thread.
//    writeSemaphore.acquire()
//
//    this.cmd = cmd.getBytes
//    readied = true
//
//    //read data
//    lockRead.synchronized(lockRead.notify())
//  }

  override def read: Int =  {
    var rst: Int = -1

    if(!readied) {
      lockRead.synchronized(lockRead.wait())
    }

    if (curIndex == cmd.length + 1) {
      rst = -1
      readied = false
      curIndex = 0
//      println(s"read complete - ${cmd.map(_.toChar).mkString}")
      readCompleteObv.onNext()
      writeSemaphore.release()
    }

  //todo lock read operation until former cmd return
//    println("read: Int = lock.synchronized - " + curIndex + "cmdLength - " + cmd.length)
    if (readied && curIndex < cmd.length) { //readied the cmd
      rst = cmd(curIndex)

      curIndex += 1
//      println("cmd readied && curIndex <= cmd.length -" + curIndex)

    }
    //append '\n'
    else if (readied && curIndex == cmd.length) {
//      println("(readied && curIndex == cmd.length + 1) - " + curIndex)

      rst = '\n'
//      readied = false
      curIndex += 1
    }

//    println("rst - " + rst.toChar)
    rst
  }

  override def close(): Unit = {
    super.close()
    closedMark = true
  }
}

