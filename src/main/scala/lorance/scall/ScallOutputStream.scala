package lorance.scall

import java.io.OutputStream
import rx.lang.scala.{Observable, Subject}

/**
  * read stream split by uuid
  */
class ScallOutputStream(implicit writeLock: WriteLock) extends OutputStream {
  private var readerDispatch = new ReaderDispatch()
  private val outputSub = Subject[String]()

  val outputObv: Observable[String] = outputSub

  override def write(b: Int): Unit = {
    readerDispatch.appendMsg(b.toByte).foreach(x => {
      outputSub.onNext(x)
      readerDispatch = new ReaderDispatch()
      writeLock.synchronized(writeLock.notify())
    })
  }

}

