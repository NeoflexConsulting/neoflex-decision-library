package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ Applicant, CheckMarks, CreditHistoryQuality, IntermediateValues }
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption

case class QualityFlow(
  applicant: Applicant,
  chQuality: CreditHistoryQuality,
  checkMarks: CheckMarks = CheckMarks(),
  values: IntermediateValues = IntermediateValues())
    extends Flow(
      "chq-f-1",
      "Credit history quality flow",
      flowOps(
        ValuesPrecalculating(applicant.externalCheck.loans, values),
        BadDebtCalculating(applicant.externalCheck.loans, chQuality),
        ChecksFlow(applicant, chQuality, checkMarks, values),

        rule("gech-r-1") {
          condition("Is the external credit history good?", {
            checkMarks.isAllMarksWhite && applicant.externalCheck.loans.length > 1 &&
            values.totalCreditLimit >= 500000 &&
            (values.totalCreditLimit / applicant.externalCheck.loans.length) >= 250000
          }) andThen {
            chQuality.goodExternalCreditHistory = 'Y'
          }
        },

        action("sop-a-1", "Set output parameters") {
          chQuality.amountOfPayment = values.amountOfPayment
          chQuality.totalSumCRE = values.totalSumCRE
          chQuality.maxDelinquencyOverallHistory = applicant.externalCheck.loansOverview.worstStatusEver
          chQuality.ratioPastDueOverTotalAmount = values.totalDelqBalance / values.totalSumCRE
        }
      )
    )
