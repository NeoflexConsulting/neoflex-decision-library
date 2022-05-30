package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.Functions.{ getPersonAge, isInRegion }
import ru.neoflex.ndk.strategy.domain.result.{Score, ScoringDetails}
import ru.neoflex.ndk.strategy.domain.Application

final case class CLIENT_GM_4_201908(
  application: Application,
  score: Score,
  scoreVal: ScoringDetails = ScoringDetails("CLIENT_GM_4_201908", 100),
  creditSumOverdue: CreditSumOverdue = CreditSumOverdue())
    extends Flow(
      "CLIENT_GM_4_201908",
      "CLIENT_GM_4_201908",
      flowOps(
        rule("client-gm-4-201908-r-1") {
          condition("age >= 65", getPersonAge(application) >= 65) andThen {
            scoreVal.scoreValue += 7
          } condition ("age >= 45", getPersonAge(application) >= 45) andThen {
            scoreVal.scoreValue += 10
          } condition ("age >= 18", getPersonAge(application) >= 18) andThen {
            scoreVal.scoreValue += 30
          } otherwise {
            scoreVal.scoreValue -= 2
          }
        },

        rule("client-gm-4-201908-r-2") {
          condition("region score = 1", isInRegion(application, 1)) andThen {
            scoreVal.scoreValue -= 5
          } condition ("region score = 2", isInRegion(application, 2)) andThen {
            scoreVal.scoreValue += 15
          } condition ("region score = 3", isInRegion(application, 3)) andThen {
            scoreVal.scoreValue += 25
          } otherwise {
            scoreVal.scoreValue -= 5
          }
        },

        CBACTDELCalculation(application, creditSumOverdue),

        rule("client-gm-4-201908-r-3") {
          condition("creditSumOverdue >= 200", creditSumOverdue.value >= 200) andThen {
            scoreVal.scoreValue -= 100
          } condition("creditSumOverdue >= 10", creditSumOverdue.value >= 10) andThen {
            scoreVal.scoreValue -= 10
          } condition("creditSumOverdue >= 18", creditSumOverdue.value >= 18) andThen {
            scoreVal.scoreValue += 5
          } otherwise {
            scoreVal.scoreValue -= 3
          }
        },

        action("client-gm-4-201908-a-1", "addScoreToResult") {
          score.details :+= scoreVal
        }
      )
    )
