package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.{ action, flowOps }
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.{ PersonScore, ScoringResult }
import ru.neoflex.ndk.strategy.scoring.{ ACQ_GM_4_201912, Application_4_0_SCB, Trial2ScoreDistributionFlow }

final case class ScoringV2(application: Application, result: ScoringResult)
    extends Flow(
      "sc-f-1",
      "Scoring",
      flowOps(
        ACQ_GM_4_201912(application, result.score),
        Application_4_0_SCB(application, result.score),

        action("sc-a-1", "setPrimaryScore") {
          val maxScore = result.score.details.maxBy(_.scoreValue)
          result.score.personScore = PersonScore(maxScore.scoreFunction, maxScore.scoreValue, application.person.map(_.cuid).get)
        },

        Trial2ScoreDistributionFlow(result)
      )
    )
