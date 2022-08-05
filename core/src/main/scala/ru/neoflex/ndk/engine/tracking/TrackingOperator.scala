package ru.neoflex.ndk.engine.tracking

import ru.neoflex.ndk.dsl.FlowOp

import java.time.Instant

final case class TrackingOperator(
  op: FlowOp,
  runAt: Instant,
  finishedAt: Option[Instant] = None,
  children: List[TrackingOperator] = List.empty)
