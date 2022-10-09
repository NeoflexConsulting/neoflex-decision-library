package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax.flowOps
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.ScoringResult
import ru.neoflex.ndk.strategy.scoring.Scoring

final case class ApprovalStrategyFlow(application: Application, result: ScoringResult)
    extends Flow(
      "ap-f-1",
      "Approval strategy",
      application.person.map(_.cuid.toString),
      flowOps(
        SetStrategyDetails(application, result),
        TrialSelectorSCRD(application, result),
        Scoring(application, result),
        TrialsPostScoringFlow(application, result),
        WFSelectorUnion(result)
      )
    )
