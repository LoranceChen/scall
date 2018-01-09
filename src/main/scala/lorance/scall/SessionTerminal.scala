package lorance.scall

import java.io.InputStream

import scala.util.control.Breaks._
import com.jcraft.jsch.{Channel, ChannelExec, JSch, Session}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * execute command on session's channel without context
  */
class SessionTerminal(auth: Auth, config: Config) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsch: JSch = new JSch()

  private val session: Session = jsch.getSession(auth.name.orNull, auth.host, auth.port)
  session.setConfig("StrictHostKeyChecking", "no")
  auth.key match {
    case Password(value) =>
      session.setPassword(value)
    case IdentityFile(path) =>
      jsch.addIdentity(path)
    case _ => Unit
  }

  session.connect(config.connectTimeout * 1000)

  //status
  private var execChannel: ChannelExec = _
  private var input: InputStream = _
  private var errInput: InputStream = _
  private var buffer = new ArrayBuffer[Byte]()
  private var errBuffer = new ArrayBuffer[Byte]()
  private var exitStatus: Int = 0


  private val tmpBufferSize = 1024
  private val tmp = new Array[Byte](tmpBufferSize)
  private val lockBegin = new AnyRef
  private val lockEnd = new AnyRef
  private val lockExc = new AnyRef
  private var waitFlag = true

  def exc(cmd: Cmd): Either[Error, String] = lockExc.synchronized {
    val channel: Channel = session.openChannel("exec")
    execChannel = channel.asInstanceOf[ChannelExec]
    execChannel.setCommand(cmd.content)

    //ready status
    execChannel.setInputStream(null)
    execChannel.setErrStream(null)
    input = execChannel.getInputStream
    errInput = execChannel.getErrStream
    channel.connect()

    //begin read data
    waitFlag = false
    lockBegin.synchronized(lockBegin.notify())

    //wait result
    lockEnd.synchronized(lockEnd.wait())

    //get result
    val result = if(exitStatus == 0) { //success
      Right(new String(buffer.toArray, "UTF-8"))
    } else { //fail
      Left(Error(exitStatus, new String(errBuffer.toArray, "UTF-8")))
    }

    //disconnect
    execChannel.disconnect()
    execChannel = null

    //init status
    execChannel = null
    input = null
    errInput = null
    buffer = new ArrayBuffer[Byte]()
    errBuffer = new ArrayBuffer[Byte]()
    exitStatus = 0

    //return result
    result

  }

  val initStreamThread = new Thread(() => {
    def foo() = {
      try {
        while (true) {
          //a switch
          if(waitFlag) {
            waitFlag = false
            lockBegin.synchronized(lockBegin.wait())
          }

          //start read stream result
          while (input.available > 0) {
            val i = input.read(tmp, 0, tmpBufferSize)
            if (i < 0) {
              break
            }
            buffer ++= tmp.take(i)
          }

          while (errInput.available > 0) {
            val i = errInput.read(tmp, 0, tmpBufferSize)
            if (i < 0) {
              break

            }
            errBuffer ++= tmp.take(i)
          }

          //check command complete
          if (execChannel.isClosed) {
            exitStatus = execChannel.getExitStatus
            lockEnd.synchronized(lockEnd.notify()) //command complete
            waitFlag = true //do wait
          }

        }
      } catch {
        case e: Exception =>
          logger.error("SessionTerminalStreamThread-fail: ", e)
      }
    }
    foo()
  })
  initStreamThread.setDaemon(true)
  initStreamThread.start()

  //init
//  case class UTF8CheckFailException(code: Int, error: String) extends RuntimeException(s"""code: $code, error: $error""")
//  private lazy val checkSupport_UTF8 = {
//    Thread.sleep(1000)
//    exc(Cmd("""locale | egrep 'UTF\-8|utf8'""")) match {
//      case Left(Error(code, msg)) =>
//        throw UTF8CheckFailException(code, msg)
//      case _ => Unit
//    }
//  }

}

