package lorance.scall

import scala.collection.mutable.ArrayBuffer

object DspState extends Enumeration {
  val BeginSplit = Value(1, "BegSplit")
  val Load = Value(2, "Load")
  val EndSplit = Value(3, "EndSplit")
}

/**
  * supply to output stream
  */
class ReaderDispatch(load: ArrayBuffer[Byte], var state: DspState.Value) {

  private var debugIndex = 0
  private var cmpIndex = 0
  private val spltBeginLength = MAGIC_SPLIT_BEGIN.length
  private val spltEndLength = MAGIC_SPLIT_END.length

  def this() {
    this(ArrayBuffer.empty[Byte], DspState.BeginSplit)
  }

  /**
    * good lucky ~~~~
    * @param bt
    * @return
    */
  def appendMsg(bt: Byte): Option[String] = {
    val item = bt.toChar

    debugIndex += 1
    state match {
      case DspState.BeginSplit =>
        //is match
        if(MAGIC_SPLIT_BEGIN(cmpIndex) == item) {
          cmpIndex += 1
          //is complete compare
          if(cmpIndex == spltBeginLength) {
            state = DspState.Load
            cmpIndex = 0
          }
        } else { //set cmpIndex eq 0 except item eq MAGIC_SPLIT(0)
          cmpIndex = 0
          if(MAGIC_SPLIT_BEGIN(cmpIndex) == item) {cmpIndex += 1}
        }

      case DspState.Load =>

        //save to load until bytes too large(NOT setting yet) when settings or achieve EndSplit
        if(MAGIC_SPLIT_END(cmpIndex) != item) {
          if(cmpIndex > 0) {
            load.append(MAGIC_SPLIT_END.substring(0, cmpIndex).toCharArray.map(_.toByte): _*)
            cmpIndex = 0
          }
          load.append(bt)

        } else {
          cmpIndex += 1
          //achieve EndSplit
          if(cmpIndex == spltEndLength) {
            state = DspState.EndSplit
          }
        }
    }

    state match {
      case DspState.EndSplit => Some(load.map(_.toChar).mkString)
      case _ => None
    }

  }
}
