package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.example.domain._

case class ChecksFlow(
  applicant: Applicant,
  chQuality: CreditHistoryQuality,
  checkMarks: CheckMarks,
  values: IntermediateValues)
    extends Flow(
      "cf-1",
      "Checks flow",
      flowOps(
        rule("Having credit history CRE check") {
          condition("Is credit history missing?", applicant.chEnquiryStatus == CreditHistoryMissing) andThen {
            checkMarks.isCRECH = Yellow
          } otherwise {
            checkMarks.isCRECH = White
          }
        },

        rule("Current delinquency CRE check") {
          condition(
            "maxCurrentDelq >= 61 and currentDelqSum >= 150000",
            values.maxCurrentDelq >= 61 && values.currentDelqSum >= 150000
          ) andThen {
            checkMarks.curDelCRE = Blue
          } condition ("maxCurrentDelq <= 5 and currentDelqSum <= 50000", values.maxCurrentDelq <= 5 && values.currentDelqSum <= 50000) andThen {
            checkMarks.curDelCRE = White
          } otherwise {
            checkMarks.curDelCRE = Yellow
          }
        },

        rule("Max delinquency CRE check") {
          condition(
            "worstStatusEver is any of 3, 4, 5, 7, 8, 9",
            "345789".indexOf(applicant.externalCheck.loansOverview.worstStatusEver.toInt) >= 0
          ) andThen {
            checkMarks.maxDelCRE = Blue
          } condition ("worstStatusEver is 2", applicant.externalCheck.loansOverview.worstStatusEver == '2') andThen {
            checkMarks.maxDelCRE = Yellow
          } otherwise {
            checkMarks.maxDelCRE = White
          }
        },
        gateway("tsg-1", "Total sum CRE check") {
          when("Legal applicant")(applicant.applicantType == "LegalApplicant") andThen {
            rule("Legal applicant total sum") {
              condition("totalSumCRE > 10000000", values.totalSumCRE > 10000000) andThen {
                checkMarks.totalSumCRE = Yellow
              } otherwise {
                checkMarks.totalSumCRE = White
              }
            }
          } otherwise {
            rule("Natural applicant total sum") {
              condition("totalSumCRE > 5000000", values.totalSumCRE > 5000000) andThen {
                checkMarks.totalSumCRE = Yellow
              } otherwise {
                checkMarks.totalSumCRE = White
              }
            }
          }
        },

        rule("Bad debt CRE check") {
          condition("Is bad debt?", chQuality.isBadDebtCRE == 'Y') andThen {
            checkMarks.badDebtCRE = Blue
          } otherwise {
            checkMarks.badDebtCRE = White
          }
        }
      )
    )
