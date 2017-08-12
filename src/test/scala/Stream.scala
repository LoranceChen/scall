import java.io.InputStream


/**
  * just for learn jsch
  */
class Stream extends InputStream {
//  val cmd = s"""bash -c "ls";bash -c "cd ../"\nbash -c "ls"\nbash -c "sshpass -p ' ' ssh lorancechen@192.168.1.149"\nbash -c "ls"\nexit\n\nbash -c "ls"\n """.toCharArray
  val cmd = s"""ls\n"""//;bash -c "cd ../"\nbash -c "ls"\nbash -c "sshpass -p ' ' ssh lorancechen@192.168.1.149"\nbash -c "ls"\nexit\n\nbash -c "ls"\n """.toCharArray
  var index = 0
  val length = cmd.length

  var firstN = true
  override def read(): Int = {
    if(index >= length)
      -1 //block
    var rst: Int = 0

    if(index < length) {
      val cur = cmd(index)
      index += 1
      println(s"read - $cur")
      rst = cur.toInt
    } else if(index == length){
      index += 1
      val cur = '\n'.toInt
//      println(s"read - $cur")
      rst = cur
    }

    rst
  }

}
