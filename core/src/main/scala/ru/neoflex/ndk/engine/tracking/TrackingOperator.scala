package ru.neoflex.ndk.engine.tracking

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.engine.observer.ExecutionDetails

import java.time.Instant

final case class TrackingOperator(
  op: FlowOp,
  runAt: Instant,
  executionDetails: Option[ExecutionDetails] = None,
  finishedAt: Option[Instant] = None,
  children: List[TrackingOperator] = List.empty)
