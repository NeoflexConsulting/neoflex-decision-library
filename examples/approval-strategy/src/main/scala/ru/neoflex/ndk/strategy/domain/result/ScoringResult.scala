package ru.neoflex.ndk.strategy.domain.result

import java.time.Instant

final case class ScoringResult(
  var strategyVersion: String = "",
  var strategyType: String = "",
  var strategyName: String = "",
  var strategyVersionDate: Instant = null,
  var strategyFlow: String = "",
  var score: Score = Score(),
  var workflowCode: String = "",
  var workflowLineId: String = "",
  var rejectReason: String = "",
  var finalRiskGroup: String = "",
  var trials: Seq[Trial] = Seq.empty)
