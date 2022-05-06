package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ CreditHistoryQuality, Loan }

case class BadDebtCalculating(loans: Seq[Loan], chQuality: CreditHistoryQuality)
    extends Flow(
      "bdc-f-1",
      Some("Bad debt calculation"),
      forEachOp(NoId, Some("Has more values to determine bad debt?"), loans) { loan =>
        flow(
          forEachOp("Payment discipline", loan.paymentDiscipline) { mannerOfPayment =>
            rule("manner of payment") {
              condition("5789" contains mannerOfPayment) andThen {
                chQuality.isBadDebtCRE = 'Y'
              }
            }
          },
          rule("loan state") {
            condition(loan.loanState > 21) andThen {
              chQuality.isBadDebtCRE = 'Y'
            }
          }
        )
      }
    )
