package lorance.scall

import java.util.concurrent.Semaphore

import com.jcraft.jsch.{Channel, JSch, Session}
import org.slf4j.LoggerFactory

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
  new Thread(() => {
    while(true) {
      logger.debug("is connecting - " + session.isConnected)
      Thread.sleep(7000)
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
