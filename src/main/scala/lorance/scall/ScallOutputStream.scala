package lorance.scall

import java.io.OutputStream

import org.slf4j.LoggerFactory
import rx.lang.scala.{Observable, Subject}

case class ParsedProto(data: String, code: Int, hostLevel: Int)

/**
  * read stream split by uuid
  */
class ScallOutputStream(writeLock: WriteLock) extends OutputStream {
  private implicit val logger = LoggerFactory.getLogger(this.getClass)

//  private var readerDispatch = new ReaderDispatch()
  private val readerDispatch = new ReaderDispatchPair
  private val outputSub = Subject[ParsedProto]()

  val outputObv: Observable[ParsedProto] = outputSub

  override def write(b: Int): Unit = {
    logger.info(b.toChar.toString)

    readerDispatch.appendMsg(b.toByte).foreach(protoData => {
      val parsedProto = parseProtoData(protoData)
      outputSub.onNext(parsedProto)
      //      readerDispatch = new ReaderDispatch()
      writeLock.synchronized(writeLock.notify())
    })

  }

  private val regex = defStr2UTF8("""(?s)(.*)\n(.*)""").r
  private val onlyDigitsRegex = defStr2UTF8("^(\\d+)$").r

  def parseProtoData(protoData: ProtoData) = {
    val ProtoData(split, rstLevelId) = protoData

    val (result, code) = split match {
      case onlyDigitsRegex(cde)  =>
        ("", cde.toInt)
      case regex(rst, cde) =>
        (rst, cde.toInt)
    }

    ParsedProto(result, code, rstLevelId)
  }

}

