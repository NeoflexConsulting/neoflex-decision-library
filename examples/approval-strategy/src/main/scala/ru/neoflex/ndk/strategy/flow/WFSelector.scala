package ru.neoflex.ndk.strategy.flow

import cats.syntax.option._
import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax.{ condition, rule }
import ru.neoflex.ndk.strategy.domain.result.Trial

final case class WFSelector(trial: Trial)
    extends Flow("WFSelector-f-1", "WFSelector".some, rule("WFSelector-r-1") {
      condition("HC Hit".some, trial.hcDetails.list.split(";").length > 0 && trial.hcDetails.list.nonEmpty) andThen {
        trial.wfDetails.decision = "REJECT"
        trial.wfDetails.lineId = "HC_REJECT"
        trial.wfDetails.rejectReason = "HC"
      } condition ("riskGroup = 9".some, trial.rgDetails.riskGroup == 9) andThen {
        trial.wfDetails.decision = "REJECT"
        trial.wfDetails.lineId = "SCO_REJECT"
        trial.wfDetails.rejectReason = "SCO"
      } condition ("riskGroup = 0".some, trial.rgDetails.riskGroup == 0) andThen {
        trial.wfDetails.decision = "REJECT"
        trial.wfDetails.lineId = "SCOFR_REJECT"
        trial.wfDetails.rejectReason = "SCOFR"
      } condition ("Limit hit".some, trial.limitDetails.hit) andThen {
        trial.wfDetails.decision = "REJECT"
        trial.wfDetails.lineId = "LIMIT_REJECT"
        trial.wfDetails.rejectReason = "LIMIT"
      } otherwise {
        trial.wfDetails.decision = "CONTINUE"
        trial.wfDetails.lineId = "ELSE_CONTINUE"
      }
    })
