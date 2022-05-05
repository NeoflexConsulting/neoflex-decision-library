package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ Applicant, CheckMarks, CreditHistoryQuality, IntermediateValues }

case class QualityFlow(
  applicant: Applicant,
  chQuality: CreditHistoryQuality,
  checkMarks: CheckMarks = CheckMarks(),
  values: IntermediateValues = IntermediateValues())
    extends Flow(
      "Credit history quality flow",
      flowOps(
        ValuesPrecalculating(applicant.externalCheck.loans, values),
        BadDebtCalculating(applicant.externalCheck.loans, chQuality),
        ChecksFlow(applicant, chQuality, checkMarks, values),

        rule("Good external credit history check") {
          condition {
            checkMarks.isAllMarksWhite && applicant.externalCheck.loans.length > 1 &&
            values.totalCreditLimit >= 500000 &&
            (values.totalCreditLimit / applicant.externalCheck.loans.length) >= 250000
          } andThen {
            chQuality.goodExternalCreditHistory = 'Y'
          }
        },

        action("Set output parameters") {
          chQuality.amountOfPayment = values.amountOfPayment
          chQuality.totalSumCRE = values.totalSumCRE
          chQuality.maxDelinquencyOverallHistory = applicant.externalCheck.loansOverview.worstStatusEver
          chQuality.ratioPastDueOverTotalAmount = values.totalDelqBalance / values.totalSumCRE
        }
      )
    )
