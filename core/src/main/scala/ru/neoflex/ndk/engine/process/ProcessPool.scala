package ru.neoflex.ndk.engine.process

import cats.implicits.{ catsStdInstancesForOption, catsSyntaxFlatMapIdOps }
import cats.syntax.either._
import ru.neoflex.ndk.tools.Logging

import java.time.Instant
import java.util.concurrent.{ ArrayBlockingQueue, BlockingQueue, ConcurrentHashMap }
import scala.jdk.CollectionConverters.{ CollectionHasAsScala, ConcurrentMapHasAsScala }
import scala.util.{ Success, Try }

class ProcessPool(processIoFactory: ProcessIoFactory = BatchedIoFactory, perProcessPoolSize: Int = 1) extends Logging {

  private val pool = new ConcurrentHashMap[ProcessKey, SingleProcessTypePool]()

  def borrowProcess(command: String, args: String*): Try[PooledProcess] = {
    val commandLine = command +: args
    val key         = ProcessKey(commandLine)
    val processPool = pool.computeIfAbsent(key, _ => new SingleProcessTypePool(key))

    processPool
      .poll()
      .map(Success.apply)
      .getOrElse(addProcessOrWaitNextAvailable(processPool))
  }

  def findObsoleteProcesses(maxTime: Instant): Set[ProcessKey] = {
    pool.asScala.flatMap {
      case (_, processPool) =>
        processPool.pool.asScala.find { p =>
          p.lastTimeUsed.isBefore(maxTime)
        }.map(_.key)
    }.toSet
  }

  def freeObsoleteProcesses(keys: Set[ProcessKey], maxTime: Instant): Unit = {
    def shutdownInactiveProcesses(oldestProcess: PooledProcess, processPool: SingleProcessTypePool) = {
      oldestProcess.tailRecM {
        case p if p.lastTimeUsed.isBefore(maxTime) =>
          logger.info(
            "Process is going to shutdown because of inactivity period, last time used: " +
              s"${p.lastTimeUsed}, maxTime: $maxTime, process: ${p.process.info()}"
          )
          processPool.reduce()
          p.shutdown()
          logger.info(s"Process shut down because of inactivity period, process: ${p.process}")
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
    logger.debug("Creating new process: {}", processPool.key.commandLine.mkString(" "))
    val pb = new ProcessBuilder(processPool.key.commandLine: _*).redirectErrorStream(true)

    for {
      process <- Try(pb.start())
      _ <- Either
            .cond(
              process.isAlive,
              (),
              new IllegalStateException(s"Process starting failed: ${processPool.key}")
            )
            .toTry
      _ = processPool.grow()
    } yield PooledProcess(processPool.key, process, processIoFactory)
  }

  def release(process: PooledProcess): Unit = pool.get(process.key).release(process)

  private class SingleProcessTypePool(
    val key: ProcessKey,
    val pool: BlockingQueue[PooledProcess] = new ArrayBlockingQueue[PooledProcess](perProcessPoolSize),
    val lock: AnyRef = new AnyRef) {

    @volatile private var currentPoolSize = 0

    def poll(): Option[PooledProcess] = Option(pool.poll())
    def take(): PooledProcess         = pool.take()

    def release(process: PooledProcess): Unit = pool.put(process.touched())

    def canGrow: Boolean = currentPoolSize < perProcessPoolSize

    def grow(): Unit = currentPoolSize += 1
    def reduce(): Unit = currentPoolSize -= 1
  }
}

final case class ProcessKey(commandLine: Seq[String])
