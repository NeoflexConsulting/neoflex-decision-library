package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax.{ flow, forEachOp }
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.ScoringResult

final case class TrialsPostScoringFlow(application: Application, result: ScoringResult)
    extends Flow(
      "TrialsPostScoringFlow-f-1",
      "TrialsPostScoringFlow",
      forEachOp("TrialsPostScoringFlow-l-1", "Has more trials?", result.trials) { trial =>
        flow(
          AssignStrategyDetails(application, trial),
          HCTable(trial),
          RGSelectorFlow(trial),
          WFSelector(trial)
        )
      }
    )
