package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.Functions.{ applicantCity, applicantRegion, hasEducation }
import ru.neoflex.ndk.strategy.dictionaries.{Predictors, RegionRiskGrade}
import ru.neoflex.ndk.strategy.domain.result.{Score, ScoringDetails}
import ru.neoflex.ndk.strategy.domain.Application

final case class ACQ_GM_4_201912(
  application: Application,
  score: Score,
  scoreVal: ScoringDetails = ScoringDetails("ACQ_GM_4_201912", 50))
    extends Flow(
      "ACQ_GM_4_201912",
      "ACQ_GM_4_201912",
      flowOps(
        rule("acq-gm-r-1") {
          condition("age >= 65", Predictors("age", "1", "application" -> application) >= 65) andThen {
            scoreVal.scoreValue += 5
          } condition ("age >= 45", Predictors("age", "1", "application" -> application) >= 45) andThen {
            scoreVal.scoreValue += 15
          } condition ("age >= 18", Predictors("age", "1", "application" -> application) >= 18) andThen {
            scoreVal.scoreValue += 25
          } otherwise {
            scoreVal.scoreValue -= 1
          }
        },

        rule("acq-gm-r-2") {
          condition("education = 1", hasEducation(application, '1')) andThen {
            scoreVal.scoreValue += 2
          } condition ("education = 3", hasEducation(application, '3')) andThen {
            scoreVal.scoreValue -= 8
          } condition ("education = 2", hasEducation(application, '2')) andThen {
            scoreVal.scoreValue += 25
          } otherwise {
            scoreVal.scoreValue -= 1
          }
        }, {
          val region = applicantRegion(application)
          val city = applicantCity(application)
          val regionRisk = RegionRiskGrade(("code" like region and ("city" like city)) or ("code" is region))

          rule("acq-gm-r-3") {
            condition("region score = 1", regionRisk == 1) andThen {
              scoreVal.scoreValue -= 5
            } condition ("region score = 2", regionRisk == 2) andThen {
              scoreVal.scoreValue += 15
            } condition ("region score = 3", regionRisk == 3) andThen {
              scoreVal.scoreValue += 25
            } otherwise {
              scoreVal.scoreValue -= 5
            }
          }
        },

        action("acq-gm-a-1", "addScoreToResult") {
          score.details :+= scoreVal
        }
      )
    )
