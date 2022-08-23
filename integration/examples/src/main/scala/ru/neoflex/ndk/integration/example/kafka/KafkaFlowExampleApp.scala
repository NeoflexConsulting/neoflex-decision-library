package ru.neoflex.ndk.integration.example.kafka

import io.circe.generic.auto._
import ru.neoflex.ndk.KafkaFlowTrackingRunner
import ru.neoflex.ndk.integration.example.domain.{ Application, ScoringResult }
import ru.neoflex.ndk.integration.example.{ ApprovalStrategyFlowV1, ApprovalStrategyFlowV2 }
import ru.neoflex.ndk.integration.kafka.{ KafkaFlowApp, KafkaRecord, StreamsConfig }

object KafkaFlowExampleApp extends KafkaFlowApp with KafkaFlowTrackingRunner {
  override def streams: StreamsConfig =
    createStream[Application, ApprovalStrategyFlowV1, ScoringResult](
      "applications",
      "results",
      r => ApprovalStrategyFlowV1(r.value),
      f => KafkaRecord(f.result)
    ) ~
      createStream[Application, ApprovalStrategyFlowV2, ScoringResult](
        "applications-v2",
        "results-v2",
        r => ApprovalStrategyFlowV2(r.value),
        f => KafkaRecord(f.result)
      )
}
