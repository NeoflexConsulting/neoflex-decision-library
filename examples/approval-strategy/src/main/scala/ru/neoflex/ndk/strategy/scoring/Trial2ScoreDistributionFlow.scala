package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.{Flow, Table}
import ru.neoflex.ndk.strategy.domain.result.{ScoringResult, Trial}

final case class Trial2ScoreDistributionFlow(result: ScoringResult)
    extends Flow(
      "Trial2ScoreDistribution-f-1",
      "Trial2ScoreDistribution",
      forEachOp("Trial2ScoreDistribution-l-1", "Has more trials?", result.trials) { trial =>
        SelectScoreCard(result, trial)
      }
    )

final case class SelectScoreCard(result: ScoringResult, trial: Trial)
    extends Table("Trial2ScoreDistribution-t-1", "Select score card", {
      expressions(
        "product" expr trial.name
      ) andConditions (
        row(eqv("TR_CL_STND")).apply("Select first card") {
          trial.scoringDetails = result.score.details.head
        },
        row(eqv("TR_CC_HOMER_PLAZA")).apply("Select first card") {
          trial.scoringDetails = result.score.details.head
        },
        row(eqv("TR_CC_TW_LG")).apply("Select last card") {
          trial.scoringDetails = result.score.details.last
        }
      )
    })
