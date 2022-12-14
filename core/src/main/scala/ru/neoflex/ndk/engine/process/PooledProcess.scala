package ru.neoflex.ndk.engine.process

import java.time.Instant
import java.util.concurrent.Future

final case class PooledProcess(
  key: ProcessKey,
  process: Process,
  processIoFactory: ProcessIoFactory,
  errorMonitoringTask: Future[_],
  lastTimeUsed: Instant = Instant.now()) {

  private val processReader = processIoFactory.createReader(process)
  private val processWriter = processIoFactory.createWriter(process)

  def getProcessWriter: ProcessWriter = processWriter

  def getProcessReader: ProcessReader = processReader

  def touched(): PooledProcess = copy(lastTimeUsed = Instant.now())

  def shutdown(): Int = {
    try {
      processReader.close()
      processWriter.close()
    } catch {
      case _: Throwable =>
    }
    process.destroy()
    process.waitFor()
  }
}
