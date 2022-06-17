package ru.neoflex.ndk.engine.process

import cats.implicits.{ catsStdInstancesForOption, catsSyntaxFlatMapIdOps }
import cats.syntax.either._
import ru.neoflex.ndk.tools.Logging

import java.time.Instant
import java.util.concurrent.{ ArrayBlockingQueue, BlockingQueue, ConcurrentHashMap, Executors, TimeUnit }
import scala.io.Source
import scala.jdk.CollectionConverters.{ CollectionHasAsScala, ConcurrentMapHasAsScala }
import scala.util.{ Success, Try, Using }

class ProcessPool(processIoFactory: ProcessIoFactory = SimpleIoFactory, perProcessPoolSize: Int = 1) extends Logging {

  private val pool = new ConcurrentHashMap[ProcessKey, SingleProcessTypePool]()

  private lazy val errorMonitoringPool = Executors.newCachedThreadPool(new NamedThreadFactory("process-err-monitor"))

  def borrowProcess(command: String, args: String*): Try[PooledProcess] = {
    val commandLine = command +: args
    val key         = ProcessKey(commandLine)
    val processPool = pool.computeIfAbsent(key, _ => new SingleProcessTypePool(key))

    processPool
      .poll()
      .map(Success.apply)
      .getOrElse(addProcessOrWaitNextAvailable(processPool))
      .map { p =>
        logger.trace("Process {} borrowed from the pool", p.process)
        p
      }
  }

  def findObsoleteProcesses(maxTime: Instant): Set[ProcessKey] = {
    pool.asScala.flatMap {
      case (_, processPool) =>
        processPool.pool.asScala.find { p =>
          p.lastTimeUsed.isBefore(maxTime)
        }.map(_.key)
    }.toSet
  }

  private def shutdownProcess(processPool: SingleProcessTypePool, p: PooledProcess, message: String): Unit = {
    processPool.reduce()
    p.errorMonitoringTask.cancel(true)
    p.shutdown()
    logger.info(s"$message: ${p.process}")
  }

  def freeObsoleteProcesses(keys: Set[ProcessKey], maxTime: Instant): Unit = {
    def shutdownInactiveProcesses(oldestProcess: PooledProcess, processPool: SingleProcessTypePool) = {
      oldestProcess.tailRecM {
        case p if p.lastTimeUsed.isBefore(maxTime) =>
          logger.info(
            "Process is going to shutdown because of inactivity period, last time used: " +
              s"${p.lastTimeUsed}, maxTime: $maxTime, process: ${p.process.info()}"
          )
          shutdownProcess(processPool, p, "Process shut down because of inactivity period, process")
          processPool.poll().map(Either.left)
        case p =>
          processPool.release(p)
          Some(Either.right(()))
      }
    }

    keys.foreach { key =>
      Option(pool.get(key)).foreach { processPool =>
        processPool.lock.synchronized {
          processPool.poll().foreach {
            shutdownInactiveProcesses(_, processPool)
          }
        }
      }
    }
  }

  private def addProcessOrWaitNextAvailable(processPool: SingleProcessTypePool): Try[PooledProcess] =
    processPool.lock.synchronized {
      processPool.poll().map(Success.apply).getOrElse {
        if (processPool.canGrow) {
          addProcess(processPool)
        } else {
          Success(processPool.take())
        }
      }
    }

  private def addProcess(processPool: SingleProcessTypePool): Try[PooledProcess] = {
    def errorStartingProcess(process: Process): Throwable = {
      def error(exitCode: Int, output: String) = {
        new IllegalStateException(
          s"Process starting failed: ${processPool.key}, exit code: $exitCode, output: $output"
        )
      }

      Using(Source.fromInputStream(process.getErrorStream)) { source =>
        val output = source.mkString
        error(process.exitValue(), output)
      }.fold(t => {
        val e = error(process.exitValue(), "")
        e.addSuppressed(t)
        e
      }, x => x)
    }

    logger.debug("Creating new process: {}", processPool.key.commandLine.mkString(" "))
    val pb = new ProcessBuilder(processPool.key.commandLine: _*)

    for {
      process             <- Try(pb.start())
      _                   <- Either.cond(!process.waitFor(2, TimeUnit.SECONDS), (), errorStartingProcess(process)).toTry
      errorMonitoringTask = errorMonitoringPool.submit(new ProcessErrorMonitoringTask(process))
      _                   = processPool.grow()
    } yield PooledProcess(processPool.key, process, processIoFactory, errorMonitoringTask)
  }

  def release(process: PooledProcess): Unit = {
    if (process.process.isAlive) {
      pool.get(process.key).release(process)
      logger.trace("Process {} returned back to the pool", process.process)
    } else {
      shutdownProcess(pool.get(process.key), process, "Process was exited before returning to the pool, process")
    }
  }

  private class SingleProcessTypePool(
    val key: ProcessKey,
    val pool: BlockingQueue[PooledProcess] = new ArrayBlockingQueue[PooledProcess](perProcessPoolSize),
    val lock: AnyRef = new AnyRef) {

    @volatile private var currentPoolSize = 0

    def poll(): Option[PooledProcess] = Option(pool.poll())
    def take(): PooledProcess         = pool.take()

    def release(process: PooledProcess): Unit = pool.put(process.touched())

    def canGrow: Boolean = currentPoolSize < perProcessPoolSize

    def grow(): Unit   = currentPoolSize += 1
    def reduce(): Unit = currentPoolSize -= 1
  }
}

final case class ProcessKey(commandLine: Seq[String])

private class ProcessErrorMonitoringTask(p: Process) extends Runnable with Logging {
  override def run(): Unit = {
    Using(Source.fromInputStream(p.getErrorStream)) { source =>
      source.getLines().foreach { line =>
        System.err.println(line)
        if (Thread.currentThread().isInterrupted) {
          return
        }
      }
    }.fold(e => logger.warn("Error in the thread which reads stderr of the process has occurred", e), x => x)
  }
}
