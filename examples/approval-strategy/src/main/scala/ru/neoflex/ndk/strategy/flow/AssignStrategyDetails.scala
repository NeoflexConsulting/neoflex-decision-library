package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.{expressions, row}
import ru.neoflex.ndk.strategy.dictionaries.Predictors
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.Trial

final case class AssignStrategyDetails(application: Application, trial: Trial)
    extends Table(
      "AssignStrategyDetails-t-1",
      "AssignStrategyDetails",
      expressions(
        "trialName" expr trial.name,
        "activeScOffer" expr application.person.activeScOffer,
        "activeRdOffer" expr application.person.activeRdOffer,
        "isNewClient" expr Predictors("isNewClient", "1", "application" -> application)
      ) andConditions (
        row(eqv("TR_CL_STND"), any(), any(), contains(true)).apply("Set strategy CashStreet New") {
          trial.strategyName = "CashStreet New"
          trial.strategyFlow = "CASH_STRET_NEW"
        },
        row(eqv("TR_CL_STND"), eqv(true), any(), contains(false)).apply("Set strategy CashDM Current") {
          trial.strategyName = "CashDM Current"
          trial.strategyFlow = "CASH_DM_XSELL"
        },
        row(eqv("TR_CL_STND"), eqv(false), any(), contains(false)).apply("Set strategy CashStreet Current") {
          trial.strategyName = "CashStreet Current"
          trial.strategyFlow = "CASH_STREET_EXISTING"
        },
        row(eqv("TR_CC_TW_LG_STND"), any(), any(), contains(true)).apply("Set strategy Revolving Street New") {
          trial.strategyName = "Revolving Street New"
          trial.strategyFlow = "CARD_STREET_NEW"
        },
        row(eqv("TR_CC_TW_LG_STND"), any(), eqv(true), contains(false)).apply("Set strategy Revolving DM Current") {
          trial.strategyName = "Revolving DM Current"
          trial.strategyFlow = "CARD_DM_XSELL"
        },
        row(eqv("TR_CC_TW_LG_STND"), any(), eqv(false), contains(false)).apply("Set strategy Revolving Street Current") {
          trial.strategyName = "Revolving Street Current"
          trial.strategyFlow = "CARD_STREET_EXISTING"
        },
        row(eqv("TR_CC_HOMER_POLZA_STND"), any(), any(), contains(true)).apply("Set strategy Revolving Street New") {
          trial.strategyName = "Revolving Street New"
          trial.strategyFlow = "CARD_STREET_NEW"
        },
        row(eqv("TR_CC_HOMER_POLZA_STND"), any(), eqv(true), contains(false)).apply("Set strategy Revolving DM Current") {
          trial.strategyName = "Revolving DM Current"
          trial.strategyFlow = "CARD_DM_XSELL"
        },
        row(eqv("TR_CC_HOMER_POLZA_STND"), any(), eqv(false), contains(false))
          .apply("Set strategy Revolving Street Current") {
            trial.strategyName = "Revolving Street Current"
            trial.strategyFlow = "CARD_STREET_EXISTING"
          }
      )
    )
