package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.domain.result.ScoringResult

final case class WFSelectorUnion(result: ScoringResult)
    extends Table("WFSelectorUnion-t-1", "WFSelectorUnion", {
      expressions(
        "countOfTrials" expr result.trials.length,
        "countOfContinueDecisions" expr result.trials.count(_.wfDetails.decision == "CONTINUE"),
        "countOfLimitRejections" expr result.trials.count(_.wfDetails.rejectReason == "LIMIT"),
        "countOfSCORejections" expr result.trials.count(_.wfDetails.rejectReason == "SCO"),
        "countOfSCOFRRejections" expr result.trials.count(_.wfDetails.rejectReason == "SCOFR"),
        "countOfHCRejections" expr result.trials.count(_.wfDetails.rejectReason == "HC")
      ) andConditions (
        row(eqv(0), any(), any(), any(), any(), any()).apply("Reject") {
          result.workflowCode = "REJECT"
          result.workflowLineId = "NO_TRIALS_REJECT"
          result.rejectReason = "NO_OFFER"
          result.finalRiskGroup = "0"
        },
        row(any(), eqv(0), gte(1), any(), any(), any()).apply("Reject") {
          result.workflowCode = "REJECT"
          result.workflowLineId = "LIMIT_REJECT"
          result.rejectReason = "LIMIT"
          result.finalRiskGroup = "0"
        },
        row(any(), eqv(0), any(), gte(1), any(), any()).apply("Reject") {
          result.workflowCode = "REJECT"
          result.workflowLineId = "SCO_REJECT"
          result.rejectReason = "SCO"
          result.finalRiskGroup = "0"
        },
        row(any(), eqv(0), any(), any(), gte(1), any()).apply("Reject") {
          result.workflowCode = "REJECT"
          result.workflowLineId = "SCOFR_REJECT"
          result.rejectReason = "SCOFR"
          result.finalRiskGroup = "0"
        },
        row(any(), eqv(0), any(), any(), any(), gte(1)).apply("Reject") {
          result.workflowCode = "REJECT"
          result.workflowLineId = "HC_REJECT"
          result.rejectReason = "HC"
          result.finalRiskGroup = "0"
        },
        row(any(), gte(1), any(), any(), any(), any()).apply("Accept") {
          result.workflowCode = "APPROVE"
          result.workflowLineId = "REGULAR_APPROVE"
          result.finalRiskGroup = "1"
        },
      )
    })
