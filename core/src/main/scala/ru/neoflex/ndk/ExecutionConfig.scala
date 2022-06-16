package ru.neoflex.ndk

import ru.neoflex.ndk.engine.process.{ BatchedIoFactory, ProcessIoFactory }

import scala.concurrent.duration.{ Duration, DurationInt }

final case class ExecutionConfig(rest: RestConfig, processPool: ProcessPoolConfig)
final case class RestConfig(endpoints: Map[String, String])
final case class ProcessPoolConfig(
  processIoFactory: ProcessIoFactory = BatchedIoFactory,
  perProcessPoolSize: Int = 1,
  processKeepAliveTime: Duration = 5 minutes)

object ExecutionConfig {
  val Empty: ExecutionConfig = ExecutionConfig(RestConfig(Map.empty), ProcessPoolConfig())
}
