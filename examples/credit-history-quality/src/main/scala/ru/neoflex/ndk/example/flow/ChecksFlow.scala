package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain._

case class ChecksFlow(
  applicant: Applicant,
  chQuality: CreditHistoryQuality,
  checkMarks: CheckMarks,
  values: IntermediateValues)
    extends Flow(
      "Checks flow",
      flowOps(
        rule("Having credit history CRE check") {
          condition(applicant.chEnquiryStatus == CreditHistoryMissing) andThen {
            checkMarks.isCRECH = Yellow
          } otherwise {
            checkMarks.isCRECH = White
          }
        },

        rule("Current delinquency CRE check") {
          condition(values.maxCurrentDelq >= 61 && values.currentDelqSum >= 150000) andThen {
            checkMarks.curDelCRE = Blue
          } condition (values.maxCurrentDelq <= 5 && values.currentDelqSum <= 50000) andThen {
            checkMarks.curDelCRE = White
          } otherwise {
            checkMarks.curDelCRE = Yellow
          }
        },

        rule("Max delinquency CRE check") {
          condition("345789" contains applicant.externalCheck.loansOverview.worstStatusEver) andThen {
            checkMarks.maxDelCRE = Blue
          } condition(applicant.externalCheck.loansOverview.worstStatusEver == '2') andThen {
            checkMarks.maxDelCRE = Yellow
          } otherwise {
            checkMarks.maxDelCRE = White
          }
        },

        gateway("Total sum CRE check") {
          when(applicant.applicantType == "LegalApplicant") andThen {
            rule("Legal applicant total sum") {
              condition(values.totalSumCRE > 10000000) andThen {
                checkMarks.totalSumCRE = Yellow
              } otherwise {
                checkMarks.totalSumCRE = White
              }
            }
          } otherwise {
            rule("Natural applicant total sum") {
              condition(values.totalSumCRE > 5000000) andThen {
                checkMarks.totalSumCRE = Yellow
              } otherwise {
                checkMarks.totalSumCRE = White
              }
            }
          }
        },

        rule("Bad debt CRE check") {
          condition(chQuality.isBadDebtCRE == 'Y') andThen {
            checkMarks.badDebtCRE = Blue
          } otherwise {
            checkMarks.badDebtCRE = White
          }
        }
      )
    )
