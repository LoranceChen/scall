package lorance.scall

import java.util.concurrent.Semaphore

import com.jcraft.jsch.{Channel, JSch, Session}
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, Promise}

case class WriteLock()

/**
  *
  */
sealed class JSchWrapper(auth: Auth, config: Config) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsch = new JSch()
  private val session: Session = jsch.getSession(auth.name, auth.host, auth.port)

  private val writeLock = WriteLock()
  private val writeSemaphore = new Semaphore(1)

  private val disconnectFuture = Promise[Auth]

  val onDisconnect: Future[Auth] = disconnectFuture.future
  val scallOutputStream = new ScallOutputStream(writeLock)
  val scallErrorStream = new ScallErrorStream()
  val scallInputStream = new ScallInputStream(scallOutputStream)(writeSemaphore, writeLock)

  auth.key match {
    case Password(value) =>
      session.setPassword(value)
    case IdentityFile(path) => //todo: Is addIdentity before getSession?
      jsch.addIdentity(path)
    case _ => Unit
  }

  session.setConfig("StrictHostKeyChecking", "no")
  session.setServerAliveInterval(config.serverAliveInterval * 1000)
  session.setServerAliveCountMax(2)
  session.connect(config.connectTimeout * 1000) // making a connection with timeout.

  //每10秒检查一次连接状况
  new Thread(new Runnable(){
    override def run() = {
      while(session.isConnected) {
        Thread.sleep(10 * 1000) //10s
      }

      disconnectFuture.trySuccess(auth)
    }
  }).start()

  private val channel: Channel = session.openChannel("shell")

  import com.jcraft.jsch.ChannelShell

//  channel.asInstanceOf[ChannelShell].setPtyType("dumb") // remove special effect, such as color
  val channelShell = channel.asInstanceOf[ChannelShell]
  channelShell.setPty(false)
//  channelShell.setEnv("LC_ALL", "en_US.UTF-8")
//  channelShell.setEnv("LC_ALL", "en_US.utf8")

  // Enable agent-forwarding.
  //((ChannelShell)channel).setAgentForwarding(true);
  channel.setInputStream(scallInputStream)
  channel.setOutputStream(scallOutputStream)
  channel.setExtOutputStream(scallErrorStream)
  channel.connect(config.connectTimeout * 1000)

  def close() = {
    scallInputStream.close()
    scallOutputStream.close()
    channel.disconnect()
    session.disconnect()
  }
}
