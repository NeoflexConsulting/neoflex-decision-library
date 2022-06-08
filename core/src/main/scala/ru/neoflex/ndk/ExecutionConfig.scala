package ru.neoflex.ndk

final case class ExecutionConfig(rest: RestConfig)
final case class RestConfig(endpoints: Map[String, String])

object ExecutionConfig {
  val Empty: ExecutionConfig = ExecutionConfig(RestConfig(Map.empty))
}


