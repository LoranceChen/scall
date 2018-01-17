//package lorance.scall
//
//import java.io.{File, InputStream}
//import java.util.UUID
//import java.util.concurrent.{Executors, Semaphore}
//
//import com.jcraft.jsch.{Channel, ChannelExec, JSch, Session}
//import org.slf4j.LoggerFactory
//
//import scala.collection.mutable.ArrayBuffer
//import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future, Promise}
//import scala.util.control.Breaks._
//import scala.util.control.NonFatal
//
///**
//  * execute command on session's channel without context
//  */
//class SessionTerminalPool(auth: Auth, config: Config, poolSize: Int = Runtime.getRuntime.availableProcessors) {
//  private val logger = LoggerFactory.getLogger(this.getClass)
//  private val jsch: JSch = new JSch()
//
//  private val session: Session = jsch.getSession(auth.name.orNull, auth.host, auth.port)
//  session.setDaemonThread(true)
//  session.setConfig("StrictHostKeyChecking", "no")
//  auth.key match {
//    case Password(value) =>
//      session.setPassword(value)
//    case IdentityFile(path) =>
//      jsch.addIdentity(path)
//    case _ => Unit
//  }
//  session.connect(config.connectTimeout * 1000 * 5)
//
////  val asyncLock = new AnyRef
//
//  val threadPool: ExecutionContext = ExecutionContext.fromExecutorService(
//    Executors.newWorkStealingPool(poolSize)
//  )
//  object ThreadIdQueue {
//    private val usableStreamThreadQueue = {
//      val q = collection.mutable.Queue[Int]()
//      q.enqueue(0 until poolSize: _*)
//      q
//    }
//
//    private val queueLock = new AnyRef
//    def enqueue(id: Int): Unit = queueLock.synchronized {
//      usableStreamThreadQueue.enqueue(id)
//
//    }
//
//    def dequeue: Int = queueLock.synchronized {
//      usableStreamThreadQueue.dequeue
//    }
//
//  }
//
//  private val streamSemaphore = new Semaphore(poolSize)
//
//  //  protected
//  class SessionStreamThread extends Thread {
//    setDaemon(true)
//    @volatile var uuid: String = _
//    @volatile var execChannel: ChannelExec = _
//    @volatile var input: InputStream = _
//    @volatile var errInput: InputStream = _
//    @volatile var buffer = new ArrayBuffer[Byte](1024)
//    @volatile var errBuffer = new ArrayBuffer[Byte](1024)
//    @volatile var exitStatus: Int = -1000
//
//
//    val tmpBufferSize = 2
//    val tmp = new Array[Byte](tmpBufferSize)
//    val lockBegin = new AnyRef
//    val lockEnd = new AnyRef
//    @volatile var waitFlag = true
//
//    @volatile var resultPromise: Promise[StreamRstData] = Promise[StreamRstData]
//
//    override def run(): Unit = {
//      try {
//        while (true) {
//          //a switch
//          if (waitFlag) {
//            waitFlag = false
//            //init status
//            //            buffer = new ArrayBuffer[Byte]()
//            //            errBuffer = new ArrayBuffer[Byte]()
//            //            exitStatus = 0
//            //            resultPromise = Promise[StreamRstData]
//
//            lockBegin.synchronized(lockBegin.wait())
//          }
//
//          //start read stream result
//          breakable {
//            while (input.available > 0) {
//              val i = input.read(tmp, 0, tmpBufferSize)
//              if (i < 0) {
//                break
//              }
//              buffer ++= tmp.take(i)
//            }
//          }
////          println("before-get-buffer: " + new String(buffer.toArray, "UTF-8"))
//
//
//          breakable {
//            while (errInput.available > 0) {
//              val i = errInput.read(tmp, 0, tmpBufferSize)
//              if (i < 0) {
//                break
//              }
//              errBuffer ++= tmp.take(i)
//            }
//          }
//
//          //check command complete
//          if (execChannel.isClosed) {
//            exitStatus = execChannel.getExitStatus
//            waitFlag = true //do wait
//
//            println(s"$uuid - get-buffer: " + new String(buffer.toArray, "UTF-8") + " - " + System.currentTimeMillis())
////            val rst = StreamRstData(buffer, errBuffer, exitStatus)
//            //            buffer = new ArrayBuffer[Byte]()
//            //            errBuffer = new ArrayBuffer[Byte]()
//            //            exitStatus = 0
//            lockEnd.synchronized(lockEnd.notify()) //command complete
////            resultPromise.trySuccess(rst)
//
//          }
//
//        }
//      } catch {
//        case e: Exception =>
//          println("Exception-SessionTerminalStreamThread-fail:" + e.getStackTraceString)
//          logger.error("Exception-SessionTerminalStreamThread-fail: ", e)
//      }
//    }
//
//  }
//
//  private val streamThreadPool: Map[Int, SessionStreamThread] = {
//    (0 until poolSize).map(index => {
//      val t = new SessionStreamThread
//      t.start()
//      index -> t
//    }).toMap
//
//  }
//
//  case class StreamRstData(buffer: ArrayBuffer[Byte], errBuffer: ArrayBuffer[Byte], exitStatus: Int)
//
//  //  val tmpTestLock = new AnyRef
////  var poolIndex = 0
////  def excAsync(cmd: Cmd): Future[Either[Error, String]] = {
//  def excAsync(cmd: Cmd): Either[Error, String] = {
//    implicit val threadPl: ExecutionContext = threadPool
//
////    Thread.sleep(20)
//    val reqId = UUID.randomUUID().toString
//    streamSemaphore.acquire()
//
//  //  def excAsync(cmd: Cmd): Either[Error, String] = {
////    Future({
//      /**
//        * limit resource
//        * ensure there have available resource
//        * block threads if too many request
//        * Notice: Semaphore not promise thread safe
//        */
//
//      /**
//        * get next InputStreamThread from pool
//        * get thread safely
//        */
//
//      /**
//        * reconsider a new way to use streamThreadPool
//        * 现在的问题是：不是说获取到了信号量就可以处理业务了。因为释放的条件是Stream中发送过消息，
//        * 这时候初始化在没有完成。
//        */
//      val (streamThreadId, thread) = {//asyncLock.synchronized(
//        /**
//          * find the right thread
//          */
//        val id = ThreadIdQueue.dequeue
//        val t = streamThreadPool(id)
//        println(s"$reqId - dequeue - " + id + " - " + System.currentTimeMillis())
//
//        (id, t)
//      }
//
//
//      import thread._
//
//      assert(execChannel == null)
//
//      val channel: Channel = session.openChannel("exec")
//      execChannel = channel.asInstanceOf[ChannelExec]
//      execChannel.setCommand(cmd.content)
//
//      //ready status
//      uuid = reqId
//      execChannel.setInputStream(null)
//      execChannel.setErrStream(null)
//      input = execChannel.getInputStream
//      errInput = execChannel.getErrStream
//
//      //todo
//      channel.connect(config.connectTimeout * 1000 * 1000)
//
//
//      //init stat
//      buffer = new ArrayBuffer[Byte](256)
//      errBuffer = new ArrayBuffer[Byte]()
//      exitStatus = -1000
//
//  //begin read data
//      waitFlag = false
//      lockBegin.synchronized(lockBegin.notify())
//
//      //wait result
//      lockEnd.synchronized(lockEnd.wait())
////      Thread.sleep(25)
//      //      resultPromise.future.map {rstData =>
//      //        try {
//      //          val rBuffer = rstData.buffer
//      val rBuffer = buffer
//      //          val rErrBuffer = rstData.errBuffer
//      val rErrBuffer = errBuffer
//      //          val rExitStatus = rstData.exitStatus
//      val rExitStatus = exitStatus
//      val result = if (rExitStatus == 0) { //success
//        val errInfo = new String(rErrBuffer.toArray, "UTF-8")
//        Right(reqId + " - " + new String(rBuffer.toArray, "UTF-8") + " -:- " + errInfo)
//      } else { //fail
//        Left(Error(rExitStatus, new String(rErrBuffer.toArray, "UTF-8")))
//      }
//      //        //get result
//      //        val result = if (exitStatus == 0) { //success
//      //          Right(new String(buffer.toArray, "UTF-8"))
//      //        } else { //fail
//      //          Left(Error(exitStatus, new String(errBuffer.toArray, "UTF-8")))
//      //        }
//
//      //disconnect
//      execChannel.disconnect()
//      execChannel = null
//
//      //          //init status
////      uuid = null
////      execChannel = null
////      input = null
////      errInput = null
////      buffer = new ArrayBuffer[Byte](256)
////      errBuffer = new ArrayBuffer[Byte]()
////      exitStatus = -1000
//
////      resultPromise = Promise[StreamRstData]
//
//      ThreadIdQueue.enqueue(streamThreadId)
//      println(s"$reqId enqueue - $streamThreadId, " + " - " + System.currentTimeMillis())
//      streamSemaphore.release()
//
//      //          println(s"$reqId complete - " + streamThreadId)
//      //return result
//      result
//      //        } catch {
//      //          case NonFatal(e) =>
//      //            println("Exception-errrrrrrrrrrrrrrrrror111")
//      //            throw e
//      //        }
//      //      }(concurrent.ExecutionContext.Implicits.global)
//
//      //    }
////    })//75852bd9c4ce
//////    .flatten
////    .recover{
////      case NonFatal(e) =>
////        println("Exception-errrrrrrrrrrrrrrrrror222"+ e.getStackTraceString)
////
//////        logger.error("AsyncError: ", e)
////        throw e
////    }(concurrent.ExecutionContext.Implicits.global)
//  }
//
//
//
//
//
//}
//
//
//
//object SessionPoolTest extends App {
////  import scala.concurrent.ExecutionContext.Implicits.global
//  val sessionTerminal = new SessionTerminalPool(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2), 15)
//
//  val it2 = (1 to 300).toList
//  println("begin-time: " + System.currentTimeMillis())
//  val x2 = it2.toParArray.map(_ => {
//    //    new Thread(() => {
//    //      sessionTerminal.excAsync(Cmd("""cddsadsa ./not-exist-dir11111 && pwd""")).
//    val xx = sessionTerminal.excAsync(Cmd("""cd ~/tmp && pwd""")) //.
//
//    println(xx +  " - "+ System.currentTimeMillis())
////    xx.foreach(m => println(
////      m + "\n" + "time - " + System.currentTimeMillis()
////    ))(scala.concurrent.ExecutionContext.Implicits.global)
//
//    xx
//  })
//
////  Thread.sleep(1000 * 10)
////  val it = (1 to 3000).toList
////  println("begin-time: " + System.currentTimeMillis())
////  val x = it.map(_ => {
////    //    new Thread(() => {
////    //      sessionTerminal.excAsync(Cmd("""cddsadsa ./not-exist-dir11111 && pwd""")).
////    val xx = sessionTerminal.excAsync(Cmd("""cd ~/tmp && pwd""")) //.
////
////    xx.foreach(m => println(
////      m + "\n" + "time - " + System.currentTimeMillis()
////    ))(scala.concurrent.ExecutionContext.Implicits.global)
////
////    xx
////  })
//
//  Thread.currentThread().join()
//}
//
//object SystemCall extends App {
//  import sys.process._
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  val x = Future(
//    Process("ls -al", new File("/Users/lorancechen/tmp")).!!
//  )
//
//  (1 to 1000).toList.map {_ =>
//    Future{
//      Process("pwd", new File("/Users/lorancechen/tmp")).!!
//      println(System.currentTimeMillis())
//    }
//  }
//
//
//  {
//    implicit val threadPool: ExecutionContext = ExecutionContext.fromExecutorService(
//      Executors.newWorkStealingPool(20)
//    )
//    Thread.sleep(10 * 1000)
//
//    println("begin-time: " + System.currentTimeMillis())
//    val xxxx = (1 to 3000).toList.map { _ =>
//      Future {
//        val r = Process("pwd", new File("/Users/lorancechen/tmp")).!!
//        println(s"r - $r - " + System.currentTimeMillis())
//      }(threadPool)
//    }
//
//    xxxx
//  }
////  val ls = Process("whoami").!!
//
//
//
//
//  Thread.currentThread().join()
//
//}
//
//
