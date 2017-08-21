package lorance.scall

import java.io.OutputStream


import scala.collection.mutable.ArrayBuffer

/**
  * read stream split by uuid
  */
class ScallErrorStream() extends OutputStream {
  private val resultHolder = ArrayBuffer.empty[Char]

  def flashErrorMsg = {
    val rst = resultHolder.mkString
    resultHolder.clear()
    rst
  }

  override def write(b: Int): Unit = {
//    print(b.toChar.toString)
    resultHolder.append(b.toChar)
  }

}

