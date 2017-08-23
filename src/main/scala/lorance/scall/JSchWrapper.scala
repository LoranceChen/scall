package lorance.scall

import java.util.concurrent.Semaphore

import com.jcraft.jsch.{Channel, JSch, Session}

case class WriteLock()

/**
  *
  */
sealed class JSchWrapper(auth: Auth, config: Config) {
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
  }

  session.setConfig("StrictHostKeyChecking", "no")
  session.connect(config.connectTimeout * 1000) // making a connection with timeout.

  private val channel: Channel = session.openChannel("shell")

  import com.jcraft.jsch.ChannelShell

//  channel.asInstanceOf[ChannelShell].setPtyType("dumb") // remove special effect, such as color
  channel.asInstanceOf[ChannelShell].setPty(false)
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
