package lorance.scall

import org.slf4j.LoggerFactory

/**
  * 三个状态，Begin, Load, End.
  * Begin由Begin_Split组成
  * End由End_Split, hostLevel, EndProtocol组成
  * 两个dispatch中，总会有一个处于Begin,另一个处于Load状态。
  * 初始化是，第一个Dispatch的状态的Begin,另一个Dispatch的状态为Load
  *
  * Global维护当前的hostLevel和BeginCount的值
  *
  */
class ReaderDispatchPair {
  private val logger = LoggerFactory.getLogger(getClass)

  // begin
  var firstReaderDispatch = new ReaderDispatch(state = DspState.BeginSplit)
  var secondReaderDispatch = new ReaderDispatch(state = DspState.Load)

  //总会有一个处于Begin,另一个处于Load状态
  var d1CurState = firstReaderDispatch.state
  var d2CurState = secondReaderDispatch.state

  //  case class Global(var hostLevel: Int = 0, var beginCount: Int = 0)
  //  val global = Global()

  def appendMsg(byte: Byte): Option[ProtoData] = {
    logger.info(byte.toChar.toString)
    //append message
    val dspRst1 = firstReaderDispatch.appendMsg(byte)
    val dspRst2 = secondReaderDispatch.appendMsg(byte)

    //get state after receive message
    val D1AfterState = firstReaderDispatch.state
    val D2AfterState = secondReaderDispatch.state

    //check state changing
    (d1CurState, d2CurState) match {
      case (DspState.BeginSplit, DspState.Load) =>
        (D1AfterState, D2AfterState) match {
          case (DspState.Load, DspState.Load) =>
            secondReaderDispatch = new ReaderDispatch(state = DspState.BeginSplit)
          case _ => Unit
        }
      case (DspState.BeginSplit, DspState.EndHostLevel) =>
        (D1AfterState, D2AfterState) match {
          case (DspState.BeginSplit, DspState.EndProtocol) =>
            firstReaderDispatch = new ReaderDispatch(state = DspState.BeginSplit)
            secondReaderDispatch = new ReaderDispatch(state = DspState.Load)
          case _ => Unit
        }
      case (DspState.Load, DspState.BeginSplit) =>
        (D1AfterState, D2AfterState) match {
          case (DspState.Load, DspState.Load) =>
            firstReaderDispatch = new ReaderDispatch(state = DspState.BeginSplit)
          case _ => Unit
        }
      case (DspState.EndHostLevel, DspState.BeginSplit) =>
        (D1AfterState, D2AfterState) match {
          case (DspState.EndProtocol, DspState.BeginSplit) =>
            secondReaderDispatch = new ReaderDispatch(state = DspState.BeginSplit)
            firstReaderDispatch = new ReaderDispatch(state = DspState.Load)
          case _ => Unit
        }
      case _ => Unit
    }

    logger.debug(s"d1CurState - $d1CurState, d2CurState - $d2CurState - ${byte.toChar}\n")

    //update current state
    d1CurState = firstReaderDispatch.state
    d2CurState = secondReaderDispatch.state

    logger.debug(s"D1AfterState - $D1AfterState, D2AfterState - $D2AfterState\n")

    dspRst1.orElse(dspRst2)
  }

}

object A extends App {
  val e1 = DspState.BeginSplit
  val e11 = DspState.Load
  val e2 = DspState.Load
  val e22 = DspState.Load

  val rst = (e1, e11) match {
    case (e2, e22) => "good"
    case (e2, e22) => "good"
    case _ => "bad"
  }

  rst
}