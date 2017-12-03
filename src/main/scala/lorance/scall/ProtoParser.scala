//package lorance.scall
//
//import rx.lang.scala.Observable
//
////case class ParsedProto(data: Either[Error, String], hostLevel: Int)
//
///**
//  * parse a completed protocol to easy useable data type
//  */
//class ProtoParser(scallOutputStream: ScallOutputStream, scallErrorStream: ScallErrorStream) {
//  private val regex = defStr2UTF8("""(?s)(.*)\n(.*)""").r
//  private val onlyDigitsRegex = defStr2UTF8("^(\\d+)$").r
//
//  private val outputObv = scallOutputStream.outputObv
//
//  val cmdResultStream: Observable[ParsedProto] = outputObv.map(protoData => {
//    val ProtoData(split, rstLevelId) = protoData
//
//    val (result, code) = split match {
//      case onlyDigitsRegex(cde)  =>
//        ("", cde.toInt)
//      case regex(rst, cde) =>
//        (rst, cde.toInt)
//    }
//
//    val errorMsg = scallErrorStream.flashErrorMsg
//    val data = if(code == 0) Right(result) else Left(Error(code, errorMsg))
//    ParsedProto(data, rstLevelId)
//  })
//}
//
///**
//  * check host is not exit with a fail
//  */
//object HostSecurityCheckFilter {
//  def check(formerHost: Int, cmdRst: ParsedProto): Boolean = {
//    if(cmdRst.hostLevel < formerHost) {
//      cmdRst.data match {
//        case Right(_) => true
//        case Left(_) => false
//      }
//    } else {
//      true
//    }
//
//  }
//
//}