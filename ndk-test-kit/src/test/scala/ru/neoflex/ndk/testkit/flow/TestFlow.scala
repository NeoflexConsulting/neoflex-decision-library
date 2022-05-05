package ru.neoflex.ndk.testkit.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption

case class TestFlow(applicant: Applicant, out: ApplicationResponse)
    extends Flow(
      "f1",
      "Underwriting flow",
      flowOps(
        RiskLevelTable(applicant, out),

        gateway("g1", "Risk level gateway") {
          when("Level 1") { out.riskLevel == 1 } andThen action {
            out.underwritingRequired = false
          } and when("Level 3") { out.riskLevel == 3 } andThen action {
            out.underwritingRequired = true
            out.underwritingLevel = 2
          } and when("Level 4") { out.riskLevel == 4 } andThen {
            LoanDebtUnderwritingFlow(applicant.loans, out)
          } otherwise rule("r1", "Underwriting level") {
            condition(out.scoring < 150) andThen {
              out.underwritingRequired = true
              out.underwritingLevel = 2
            } otherwise {
              out.underwritingRequired = true
              out.underwritingLevel = 1
            }
          }
        }
      )
    )
