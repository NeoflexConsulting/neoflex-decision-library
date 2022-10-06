package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.strategy.Functions.{maxCurrentSumOverdue, maxOverdueDaysLast24Months}
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.Trial

final case class HardChecksFlow(application: Application, trial: Trial)
    extends Flow(
      "HardChecks-f-1",
      "HardChecksFlow",
      flowOps(
        gateway("hc-product-g-1", "Product hard checks") {
          when("TR_CC_HOMER_POLZA_STND") { trial.name == "TR_CC_HOMER_POLZA_STND" } andThen PolzaChecks(
            application,
            trial
          ) and when("TR_CC_TW_LG_STND") { trial.name == "TR_CC_TW_LG_STND" } andThen LgChecks(
            application,
            trial
          ) otherwise ClChecks(application, trial)
        }
      )
    )

final case class LgChecks(application: Application, trial: Trial)
    extends Flow(
      "LgChecks-f-1",
      "LgChecksFlow",
      flowOps(
        action {
          trial.hcDetails.lineId += "CC_LG_HCS;"
        },

        rule("lgc-r-1", "SumOverdue check") {
          condition("SumOverdue > 300", maxCurrentSumOverdue(application) > 300) andThen {
            trial.hcDetails.list += "SUM_OVERDUE_300"
          }
        },

        rule("lgc-r-2", "Overdue days check") {
          condition("Overdue days > 60", maxOverdueDaysLast24Months(application) > 60) andThen {
            trial.hcDetails.list += "OVERDUE_DAYS_60"
          }
        }
      )
    )

final case class ClChecks(application: Application, trial: Trial)
    extends Flow(
      "ClChecks-f-1",
      "ClChecksFlow",
      flowOps(
      action {
          trial.hcDetails.lineId += "CC_CL_HCS;"
        },

        rule("clc-r-1", "SumOverdue check") {
          condition("SumOverdue > 200", maxCurrentSumOverdue(application) > 200) andThen {
            trial.hcDetails.list += "SUM_OVERDUE_200"
          }
        },

        rule("clc-r-2", "Overdue days check") {
          condition("Overdue days > 60", maxOverdueDaysLast24Months(application) > 60) andThen {
            trial.hcDetails.list += "OVERDUE_DAYS_60"
          }
        }
      )
    )

final case class PolzaChecks(application: Application, trial: Trial)
    extends Flow(
      "PolzaChecks-f-1",
      "PolzaChecksFlow",
      flowOps(
        action {
          trial.hcDetails.lineId += "CC_POLZA_HCS;"
        },

        rule("pc-r-1", "SumOverdue check") {
          condition("SumOverdue > 200", maxCurrentSumOverdue(application) > 200) andThen {
            trial.hcDetails.list += "SUM_OVERDUE_200"
          }
        },

        rule("pc-r-2", "Overdue days check") {
          condition("Overdue days > 30", maxOverdueDaysLast24Months(application) > 30) andThen {
            trial.hcDetails.list += "OVERDUE_DAYS_30"
          }
        }
      )
    )
