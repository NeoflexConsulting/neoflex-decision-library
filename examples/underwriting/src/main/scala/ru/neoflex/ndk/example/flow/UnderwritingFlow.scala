package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ Applicant, ApplicationResponse }

case class UnderwritingFlow(applicant: Applicant, out: ApplicationResponse)
    extends Flow(
      "Underwriting flow",
      flowOps(
        RiskLevelTable(applicant, out),

        gateway("Risk level gateway") {
          when("Level 1") { out.riskLevel == 1 } andThen {
            out.underwritingRequired = false
          } and when("Level 3") { out.riskLevel == 3 } andThen {
            out.underwritingRequired = true
            out.underwritingLevel = 2
          } otherwise flow(
            ScoringFlow(applicant.person, out),

            rule("Underwriting level") {
              condition(out.scoring < 150) andThen {
                out.underwritingRequired = true
                out.underwritingLevel = 2
              } otherwise {
                out.underwritingRequired = true
                out.underwritingLevel = 1
              }
            }
          )
        }
      )
    )
