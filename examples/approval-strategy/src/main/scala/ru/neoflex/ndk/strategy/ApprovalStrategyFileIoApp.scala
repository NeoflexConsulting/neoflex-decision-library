package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.strategy.domain.result.ScoringResult
import ru.neoflex.ndk.strategy.flow.ApprovalStrategyFlow
import ru.neoflex.ndk.strategy.tracking.JsonFileTrackingRunner

object ApprovalStrategyFileIoApp extends JsonFileTrackingRunner with JsonIo with App {
  val application = readApplication("application.json")
  val result      = ScoringResult()
  val flow        = ApprovalStrategyFlow(application, result)

  run(flow)
  writeResult(result, "examples/approval-strategy/runs/result.json")
}
