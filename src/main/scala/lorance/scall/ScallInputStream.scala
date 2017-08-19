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

  //todo release lock if assert fail
  def setCommand(cmd: String) = {
    writeSemaphore.acquire()

    assert(!closedMark, "Input Stream has been closed")

    this.cmd = cmd.getBytes
    readied = true

    //notify begin read data
    lockRead.synchronized(lockRead.notify())

    //wait data form OutputStream
    writeLock.synchronized(writeLock.wait())
    curResult
  }

  val noRspLock = new Object

  //todo release lock if assert fail
  def setCommandNoRsp(cmd: String) = {
    //acquire and release should under different thread.
    writeSemaphore.acquire()
    assert(!closedMark, "Input Stream has been closed")

    this.cmd = cmd.getBytes
    readied = true

    //register must before notify read
    //only register once
    readCompleteObv.first.subscribe(x => {
      noRspLock.synchronized(noRspLock.notify())
    })

    //read data
    lockRead.synchronized(lockRead.notify())

    // wait message send complete
    noRspLock.synchronized(noRspLock.wait())
  }


  override def read: Int =  {
    var rst: Int = -1

    if(!readied) {
      lockRead.synchronized(lockRead.wait())
    }

    if (curIndex == cmd.length + 1) {
      rst = -1
      readied = false
      curIndex = 0
      readCompleteObv.onNext()
      writeSemaphore.release()
    }

    if (readied && curIndex < cmd.length) { //readied the cmd
      rst = cmd(curIndex)
      curIndex += 1
    }
    else if (readied && curIndex == cmd.length) {
      rst = '\n'
      curIndex += 1
    }
    rst
  }

  override def close(): Unit = {
    super.close()
    closedMark = true
  }
}

