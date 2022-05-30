package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.{Flow, Table}
import ru.neoflex.ndk.strategy.domain.result.Trial

final case class RGSelectorFlow(trial: Trial) extends Flow("RGSelector-f-1", "RGSelector",
  flowOps(
    RGSelectorTable(trial),

    rule("RGSelector-r-1") {
      condition("Was cutoff applied?", trial.rgDetails.riskGroup != 9 && trial.scoringDetails.scoreValue < 1) andThen {
        trial.rgDetails.cutOffValue = trial.scoringDetails.scoreValue
        trial.rgDetails.riskGroup = 99
        trial.rgDetails.lineId = "ERROR CASE: NO CUTOFF WAS APPLIED"
      } condition("Can be accepted?", trial.rgDetails.riskGroup != 9) andThen {
        trial.rgDetails.riskGroup = 1
      }
    }
  )
)

final case class RGSelectorTable(trial: Trial) extends Table("RGSelector-t-1", "RGSelectorTable", {
  expressions (
    "trialName" expr trial.name,
    "strategyFlow" expr trial.strategyFlow,
    "score" expr trial.scoringDetails.scoreValue
  ) withActions (
    "reject" action { (cutOffValue, riskGroup, lineId) =>
      trial.rgDetails.cutOffValue = cutOffValue.asInstanceOf[Double]
      trial.rgDetails.riskGroup = riskGroup.asInstanceOf[Int]
      trial.rgDetails.lineId = lineId.asInstanceOf[String]
    }
  ) andConditions(
    row(eqv("TR_CL_STND"), contains("NEW"), lt(0.9561)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CL_STND_STREET")
    },
    row(eqv("TR_CL_STND"), notContains("NEW"), lt(0.9423)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CL_STND_XSELL")
    },
    row(eqv("TR_CC_TW_LG_STND"), contains("NEW"), lt(0.9123)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CC_TW_LG_STND_STREET")
    },
    row(eqv("TR_CC_TW_LG_STND"), notContains("NEW"), lt(0.9124)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CC_TW_LG_STND_STREET")
    },
    row(eqv("TR_CC_HOMER_POLZA_STND"), contains("NEW"), lt(0.9756)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CC_HOMER_POLZA_STND_STREET")
    },
    row(eqv("TR_CC_HOMER_POLZA_STND"), notContains("NEW"), lt(0.9456)).apply {
      "reject" withArgs(trial.scoringDetails.scoreValue, 9, "REJ_TR_CC_HOMER_POLZA_STND_STREET")
    }
  )
})
