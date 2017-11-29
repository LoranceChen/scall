package lorance.scall

import java.io.OutputStream
import java.nio.charset.Charset

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * read stream split by uuid
  */
class ScallErrorStream() extends OutputStream {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val resultHolder = ArrayBuffer.empty[Byte]

  def flashErrorMsg = {
    val rst = new String(resultHolder.toArray, Charset.forName("UTF-8"))
    resultHolder.clear()
    rst
  }

  override def write(b: Int): Unit = {
    logger.debug(b.toChar.toString)
//    logger.debug("["+b.toChar.toString+"]")
    resultHolder.append(b.toByte)
  }

}

