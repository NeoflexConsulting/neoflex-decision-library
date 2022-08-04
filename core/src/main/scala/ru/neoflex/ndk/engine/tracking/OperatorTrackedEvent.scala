package ru.neoflex.ndk.engine.tracking

import ru.neoflex.ndk.dsl.`type`.OperatorType

final case class OperatorTrackedEvent(
  id: String,
  name: Option[String],
  `type`: OperatorType,
  ops: List[OperatorTrackedEvent])
