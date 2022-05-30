package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.strategy.Functions.{ getPersonAge, isInRegion }
import ru.neoflex.ndk.strategy.domain.result.{Score, ScoringDetails}
import ru.neoflex.ndk.strategy.domain.Application

final case class Application_4_0_SCB(
  application: Application,
  score: Score,
  scoreVal: ScoringDetails = ScoringDetails("Application_4_0_SCB", 25))
    extends Flow(
      "Application_4_0_SCB",
      "Application_4_0_SCB",
      flowOps(
        rule("application-4-0-scb-r-1") {
          condition("age >= 65", getPersonAge(application) >= 65) andThen {
            scoreVal.scoreValue += 3
          } condition ("age >= 45", getPersonAge(application) >= 45) andThen {
            scoreVal.scoreValue += 2
          } condition ("age >= 18", getPersonAge(application) >= 18) andThen {
            scoreVal.scoreValue += 11
          } otherwise {
            scoreVal.scoreValue -= 1
          }
        },

        rule("application-4-0-scb-r-2") {
          condition("region score = 1", isInRegion(application, 1)) andThen {
            scoreVal.scoreValue -= 4
          } condition ("region score = 2", isInRegion(application, 2)) andThen {
            scoreVal.scoreValue += 5
          } condition ("region score = 3", isInRegion(application, 3)) andThen {
            scoreVal.scoreValue += 6
          } otherwise {
            scoreVal.scoreValue -= 7
          }
        },

        action("application-4-0-scb-a-1", "addScoreToResult") {
          score.details :+= scoreVal
        }
      )
    )
