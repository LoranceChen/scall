package lorance.scall

import java.util.concurrent.{Executors, Semaphore, TimeUnit}

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class SessionPool(auth: Auth,  config: Config, poolSize: Int = Runtime.getRuntime.availableProcessors, maxSessions: Int = 10) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val sessionCountLimiter = new Semaphore(maxSessions / 2)
  private val threadPoolExc: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newWorkStealingPool(poolSize / 2)
  )

  private val threadPoolWait: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newWorkStealingPool(poolSize / 2)
  )
  private val ssh = new SSHClient
  ssh.loadKnownHosts()

  ssh.setConnectTimeout(config.connectTimeout * 1000)
  ssh.setTimeout(config.serverAliveCountMax * config.serverAliveInterval * 1000)
  ssh.connect(auth.host)

  auth.key match {
    case Password(pwd) =>
      ssh.authPassword(auth.name.getOrElse(System.getProperty("user.name")), pwd)
    case IdentityFile(path) =>
      ssh.authPublickey(auth.name.getOrElse(System.getProperty("user.name")), path)
    case NonKey =>
      ssh.auth(auth.name.getOrElse(System.getProperty("user.name")))
  }

  def execAsync(cmd: Cmd): Future[Either[Error, String]] = {
    val cmdRstFur = Future{
      sessionCountLimiter.acquire()
      println("do-exec")
      val session = ssh.startSession()
      session -> session.exec(cmd.content)
    }(threadPoolExc)

    cmdRstFur.map {case (session, cmdRst) =>
      logger.debug("beginwait - " + System.currentTimeMillis())

      val data = IOUtils.readFully(cmdRst.getInputStream).toString
      val errData = IOUtils.readFully(cmdRst.getErrorStream).toString
      logger.debug("endwaitData - " + System.currentTimeMillis())

      cmdRst.getExitSignal
      cmdRst.join(3, TimeUnit.SECONDS)
      val status = cmdRst.getExitStatus
      logger.debug("endwait - " + System.currentTimeMillis())

      session.close()
      sessionCountLimiter.release()

      if (status.intValue == 0) {
        Right(data)
      } else {
        Left(Error(status, errData))
      }
    }(threadPoolWait)
  }

}
