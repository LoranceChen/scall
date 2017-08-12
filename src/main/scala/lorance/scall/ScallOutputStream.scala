package lorance.scall

import java.io.OutputStream

import rx.lang.scala.{Observable, Subject}

import scala.collection.mutable.ArrayBuffer

/**
  * read stream split by uuid
  */
class ScallOutputStream(implicit writeLock: WriteLock) extends OutputStream {
  private var readerDispatch = new ReaderDispatch()
  private val outputSub = Subject[String]()

  val outputObv: Observable[String] = outputSub

  //test
  val bf = ArrayBuffer.empty[Char]

  override def write(b: Int): Unit = {
//    print(if(b.toChar == '\r') "\\r" else b.toChar.toString)

    bf.append(b.toChar)

    readerDispatch.appendMsg(b.toByte).foreach(x => {
//      println("completed - " + x)
      outputSub.onNext(x)
      readerDispatch = new ReaderDispatch()
      writeLock.synchronized(writeLock.notify())
//      writeSemaphore.release()

    })
  }

}

