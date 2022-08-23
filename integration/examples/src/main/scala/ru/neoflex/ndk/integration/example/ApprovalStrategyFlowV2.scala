package ru.neoflex.ndk.integration.example

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.{ action, condition, flowOps, rule }
import ru.neoflex.ndk.integration.example.domain.{ Application, ScoringResult }

import scala.util.Random

final case class ApprovalStrategyFlowV2(application: Application, result: ScoringResult = ScoringResult())
    extends Flow(
      "ap-f-2",
      "Approval strategy",
      flowOps(
        rule("sr-1", "status rule") {
          condition(Random.nextBoolean()) andThen {
            result.value = 1.0
          } otherwise {
            result.value = 0.5
          }
        },
        action {
          result.clientId = application.clientId
          result.status = "Completed"
        }
      )
    ) {

  override val entityId: Option[String] = application.clientId.some
}
