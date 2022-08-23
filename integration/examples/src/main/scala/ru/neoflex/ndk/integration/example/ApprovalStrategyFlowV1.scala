package ru.neoflex.ndk.integration.example

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.{ action, flowOps }
import ru.neoflex.ndk.integration.example.domain.{ Application, ScoringResult }

final case class ApprovalStrategyFlowV1(application: Application, result: ScoringResult = ScoringResult())
    extends Flow(
      "ap-f-1",
      "Approval strategy",
      flowOps(
        action("sa-1", "status action") {
          result.value = 0.5
        },
        action {
          result.clientId = application.clientId
          result.status = "Completed"
        }
      )
    )
