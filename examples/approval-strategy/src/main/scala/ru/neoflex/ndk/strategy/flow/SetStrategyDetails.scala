package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.dictionaries.Predictors
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.ScoringResult

import java.time.Instant

final case class SetStrategyDetails(application: Application, result: ScoringResult)
    extends Table(
      "ap-t-1",
      "Set strategy details",
      expressions(
        "isNewClient" expr Predictors("isNewClient", "1", "application" -> application),
        "activeOffer" expr (application.person.activeRdOffer == 1 || application.person.activeScOffer == 1)
      ) andConditions (
        row(contains(true), any()).apply("Street New") {
          result.strategyName = "MultiApproval_Street New"
          result.strategyType = "Champion"
          result.strategyVersion = "01.10.2018"
          result.strategyVersionDate = Instant.now()
          result.strategyFlow = "STREET_NEW"
        },
        row(contains(false), eqv(true)).apply("DM Current") {
          result.strategyName = "MultiApproval_DM Current"
          result.strategyType = "Champion"
          result.strategyVersion = "01.10.2018"
          result.strategyVersionDate = Instant.now()
          result.strategyFlow = "XSELL"
        },
        row(contains(false), eqv(false)).apply("Street Current") {
          result.strategyName = "MultiApproval_Street Current"
          result.strategyType = "Champion"
          result.strategyVersion = "01.10.2018"
          result.strategyVersionDate = Instant.now()
          result.strategyFlow = "STREET_EXISTING"
        }
      )
    )
