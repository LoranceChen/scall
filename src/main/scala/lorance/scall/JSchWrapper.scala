package lorance.scall

import java.util.concurrent.Semaphore

import com.jcraft.jsch.{Channel, JSch, Session}

case class WriteLock()
/**
  *
  */
sealed class JSchWrapper(auth: Auth) {
  private val jsch = new JSch()
  private val session: Session = jsch.getSession(auth.name, auth.host, auth.port)

  private val writeLock = WriteLock()
  private val writeSemaphore = new Semaphore(1)

  val scallOutputStream = new ScallOutputStream(writeLock)
  val scallErrorStream = new ScallErrorStream()
  val scallInputStream = new ScallInputStream(scallOutputStream)(writeSemaphore, writeLock)

  session.setPassword(auth.password)
  session.setConfig("StrictHostKeyChecking", "no")
  session.connect(30000) // making a connection with timeout.

  private val channel: Channel = session.openChannel("shell")

  import com.jcraft.jsch.ChannelShell

//  channel.asInstanceOf[ChannelShell].setPtyType("dumb") // remove special effect, such as color
  channel.asInstanceOf[ChannelShell].setPty(false)
  // Enable agent-forwarding.
  //((ChannelShell)channel).setAgentForwarding(true);
  channel.setInputStream(scallInputStream)
  channel.setOutputStream(scallOutputStream)
  channel.setExtOutputStream(scallErrorStream)

  channel.connect(60 * 1000)

  def close() = {
    scallInputStream.close()
    scallOutputStream.close()
    channel.disconnect()
    session.disconnect()
  }
}
