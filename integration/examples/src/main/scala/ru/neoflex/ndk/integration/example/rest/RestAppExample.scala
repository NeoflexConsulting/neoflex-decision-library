package ru.neoflex.ndk.integration.example.rest

import io.circe.generic.auto._
import ru.neoflex.ndk.FlowRunner
import ru.neoflex.ndk.integration.example.{ ApprovalStrategyFlowV1, ApprovalStrategyFlowV2 }
import ru.neoflex.ndk.integration.example.domain.{ Application, ScoringResult }
import ru.neoflex.ndk.integration.rest.dsl.path
import ru.neoflex.ndk.integration.rest.{ RestFlowApp, SealedPath }

object RestAppExample extends RestFlowApp with FlowRunner {
  override def routes: SealedPath =
    path[Application, ApprovalStrategyFlowV1, ScoringResult](
      "/strategy/v1/approval/application",
      ApprovalStrategyFlowV1(_),
      _.result
    ) ~
      path[Application, ApprovalStrategyFlowV2, ScoringResult](
        "/strategy/v2/approval/application",
        ApprovalStrategyFlowV2(_),
        _.result
      )
}
