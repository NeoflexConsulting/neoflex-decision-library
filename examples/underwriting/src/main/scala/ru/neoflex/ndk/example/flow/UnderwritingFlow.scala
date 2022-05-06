package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.example.domain.{ Applicant, ApplicationResponse }

case class UnderwritingFlow(applicant: Applicant, out: ApplicationResponse)
    extends Flow(
      "uw-f-1",
      "Underwriting flow",
      flowOps(
        RiskLevelTable(applicant, out),

        gateway("rl-g-1", "Risk level gateway") {
          when("Level 1 or 3") { out.riskLevel != 2 } andThen {
            rule("srl-r-1") {
              condition("riskLevel = 1", out.riskLevel == 1) andThen {
                out.underwritingRequired = false
              } otherwise {
                out.underwritingRequired = true
                out.underwritingLevel = 2
              }
            }
          } otherwise flow("Underwriting by scoring value")(
            ScoringFlow(applicant.person, out),
            rule("uwl-r-1") {
              condition("scoring < 150?", out.scoring < 150) andThen {
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
