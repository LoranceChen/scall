package lorance.scall

import org.junit.Test

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class SessionPoolTest {

  @Test
  def simpleTest(): Unit = {
    val sessionPool = new SessionPool(Auth("localhost", None, 22, Password("xxx")), Config(10, 5, 2))
    val rst = sessionPool.execAsync(Cmd("sleep 3; cd ~/tmp;pwd"))
    val rst2 = sessionPool.execAsync(Cmd("sleep 3; cd ~/tmp;pwd"))

    println(Await.result(rst, 10 seconds))
    println(Await.result(rst2, 10 seconds))

  }

  /**
    * set sshd_config's MaxSessions to 1000
    */
  @Test
  def onMultiThread(): Unit = {

    val sessionPool = new SessionPool(Auth("localhost", None, 22, Password(" ")), Config(10, 5, 2), 20, 1000)
    def parTest(count: Int) = {
      println("begin milli time - " + System.currentTimeMillis())
      (1 to count).toList.map { index =>
        val rst = sessionPool.execAsync(Cmd("cd ~/tmp;pwd"))
        rst.foreach(x => println(s"end milli time - $index - ${System.currentTimeMillis()} - $x"))
        rst
      }
    }

    //max session MaxSession / 2
    Await.result(Future.sequence(parTest(300)), 100 seconds)

    Thread.sleep(6 * 1000)
    Await.result(Future.sequence(parTest(3000)), 600 seconds)
  }
}
