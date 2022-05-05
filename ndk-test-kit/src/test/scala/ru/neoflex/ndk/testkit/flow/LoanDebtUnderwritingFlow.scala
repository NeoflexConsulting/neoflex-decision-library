package ru.neoflex.ndk.testkit.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.{ condition, flowOps, forEach, rule }

case class LoanDebtUnderwritingFlow(loans: Seq[Loan], out: ApplicationResponse)
    extends Flow(
      "f2",
      "Loan debt based underwriting flow",
      flowOps(
        forEach("loop1", loans) { loan =>
          rule("loop1-r1") {
            condition(loan.totalOutstanding > 5000000) andThen {
              out.underwritingRequired = true
              out.underwritingLevel = 2
            }
          }
        }
      )
    )
