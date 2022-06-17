package ru.neoflex.ndk.engine.process

import ru.neoflex.ndk.ProcessPoolConfig
import ru.neoflex.ndk.tools.Logging

import java.time.Instant
import java.util.concurrent.{ Executors, TimeUnit }

object ProcessPoolFactory {
  private lazy val processCleanerPool = Executors.newScheduledThreadPool(
    1,
    new NamedThreadFactory("process-cleaner")
  )

  def create(poolConfig: ProcessPoolConfig): ProcessPool = {
    val processPool     = new ProcessPool(poolConfig.processIoFactory, poolConfig.perProcessPoolSize)
    val keepAliveTimeMs = poolConfig.processKeepAliveTime.toMillis
    processCleanerPool.scheduleAtFixedRate(
      new FreeObsoleteProcessesTask(processPool, keepAliveTimeMs),
      keepAliveTimeMs,
      keepAliveTimeMs,
      TimeUnit.MILLISECONDS
    )
    processPool
  }

  private class FreeObsoleteProcessesTask(pool: ProcessPool, keepAliveTimeMs: Long) extends Runnable with Logging {
    override def run(): Unit = {
      val maxTime = Instant.now().minusMillis(keepAliveTimeMs)
      logger.debug(s"Running free obsolete processes task with maxTime: $maxTime")
      val obsoleteProcesses = pool.findObsoleteProcesses(maxTime)
      logger.debug(s"Next processes will be freed: {}", obsoleteProcesses)
      pool.freeObsoleteProcesses(obsoleteProcesses, maxTime)
    }
  }
}
