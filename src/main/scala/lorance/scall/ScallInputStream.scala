package lorance.scall

import java.io.InputStream
import java.util.concurrent.Semaphore

import rx.lang.scala.Subject

/**
  * send cmd
  */
class ScallInputStream(outputStream: ScallOutputStream)
                      (writeSemaphore: Semaphore, writeLock: WriteLock) extends InputStream {
  private var readied = false
  private var cmd: Array[Byte] = Array.emptyByteArray
  private var curIndex = 0
  private var closedMark = false
  private var curResult: String = _
  outputStream.outputObv.subscribe(curResult = _)

  val readCompleteObv = Subject[Unit]()
  val lockRead = new Object()

  /**
    * make sure `setCommandMultiTimes` is atomic
    */
  val methodLock = new Object

  val noRspLock = new Object

  def setCommand(cmd: String) = methodLock.synchronized {
    writeSemaphore.acquire()
    assert(!closedMark, {
      writeSemaphore.release()
      "Input Stream has been closed"
    })

    this.cmd = cmd.getBytes
    readied = true

    //notify begin read data
    lockRead.synchronized(lockRead.notify())

    //wait data form OutputStream
    writeLock.synchronized(writeLock.wait())
    curResult
  }

  def setCommandNoRsp(cmd: String) = methodLock.synchronized {
    //acquire and release should under different thread.
    writeSemaphore.acquire()

    assert(!closedMark, {
      writeSemaphore.release()
      "Input Stream has been closed"
    })

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

  /**
    * send command at least once.
    * NOTICE: make sure it is non effect
    * @param cmd
    * @return
    */
  def setCommandMultiTimes(cmd: String) = methodLock.synchronized {
    var needResend = true
    var rst: String = ""

    outputStream.outputObv.first.subscribe(msg => {
      needResend = false
      rst = msg
    })

    while (needResend) {
      setCommandNoRsp(cmd)
      Thread.sleep(15)
    }

    rst
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

//    print(rst.toChar.toString)
    rst
  }

  override def close(): Unit = {
    super.close()
    closedMark = true
  }
}

