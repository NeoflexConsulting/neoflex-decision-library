package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.strategy.domain.result.{ PersonScore, ScoringResult }
import ru.neoflex.ndk.strategy.domain.Application

final case class Scoring(application: Application, result: ScoringResult)
    extends Flow(
      "sc-f-1",
      "Scoring",
      flowOps(
        gateway("sc-g-1", "Scoring") {
          when("isNewClient") { result.strategyFlow endsWith "_NEW" } andThen flow(
            ACQ_GM_4_201912(application, result.score),
            Application_4_0_SCB(application, result.score)
          ) otherwise flow(
            CLIENT_GM_4_201908(application, result.score),
            Application_4_0_SCB(application, result.score)
          )
        },

        action("sc-a-1", "setPrimaryScore") {
          val maxScore = result.score.details.maxBy(_.scoreValue)
          result.score.personScore =
            PersonScore(maxScore.scoreFunction, maxScore.scoreValue, application.person.map(_.cuid).get)
        },

        Trial2ScoreDistributionFlow(result)
      )
    )
