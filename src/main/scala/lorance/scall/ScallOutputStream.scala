package lorance.scall

import java.io.{OutputStream}

import org.slf4j.LoggerFactory
import rx.lang.scala.{Observable, Subject}

/**
  * read stream split by uuid
  */
class ScallOutputStream(writeLock: WriteLock) extends OutputStream {
  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  private var readerDispatch = new ReaderDispatch()
  private val outputSub = Subject[ProtoData]()

  val outputObv: Observable[ProtoData] = outputSub

  override def write(b: Int): Unit = {
    logger.info(b.toChar.toString)

    readerDispatch.appendMsg(b.toByte).foreach(x => {
      outputSub.onNext(x)
      readerDispatch = new ReaderDispatch()
      writeLock.synchronized(writeLock.notify())
    })
  }

}
