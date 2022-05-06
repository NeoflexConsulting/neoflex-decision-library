package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ CreditHistoryQuality, Loan }

case class BadDebtCalculating(loans: Seq[Loan], chQuality: CreditHistoryQuality)
    extends Flow(
      "bdc-f-1",
      Some("Bad debt calculation"),
      forEachOp(NoId, Some("Has more loans to determine bad debt existence?"), loans) { loan =>
        flow(
          forEachOp("Payment discipline", Some("More values of payment discipline?"), loan.paymentDiscipline) { mannerOfPayment =>
            rule("manner of payment") {
              condition(Some("Is manner of payment any of 5, 7, 8, 9?"), "5789" contains mannerOfPayment) andThen {
                chQuality.isBadDebtCRE = 'Y'
              }
            }
          },
          rule("loan state") {
            condition(Some("loanState > 21?"), loan.loanState > 21) andThen {
              chQuality.isBadDebtCRE = 'Y'
            }
          }
        )
      }
    )
