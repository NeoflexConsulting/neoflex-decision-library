package ru.neoflex.ndk.strategy

import io.circe.generic.auto._
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.ScoringResult
import ru.neoflex.ndk.strategy.flow.ApprovalStrategyFlow
import ru.neoflex.ndk.testkit.func.{ NdkFuncSpec, Sink, Source }
import ru.neoflex.ndk.testkit.func.implicits._

class CompareRunsSpec extends NdkFuncSpec {
  "Different flow versions runs" should "be resulted in different files" in {
    Source
      .json[Application]("application.json".resourcePath)
      .map(ApprovalStrategyFlow(_, ScoringResult()))
      .result
      .map(_.result.result)
      .withFlowTraceSink(Sink.json(filePath("flow_trace_v1.json")))
      .runWithSink(Sink.json(filePath("result_v1.json")))
      .awaitResult()

    Source
      .json[Application]("application.json".resourcePath)
      .map(ApprovalStrategyFlowV2(_, ScoringResult()))
      .result
      .map(_.result.result)
      .withFlowTraceSink(Sink.json(filePath("flow_trace_v2.json")))
      .runWithSink(Sink.json(filePath("result_v2.json")))
      .awaitResult()
  }

  private def filePath(filename: String): String = s"examples/approval-strategy/runs/compare/$filename"
}
