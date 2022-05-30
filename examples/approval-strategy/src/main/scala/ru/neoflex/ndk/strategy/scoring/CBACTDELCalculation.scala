package ru.neoflex.ndk.strategy.scoring

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.domain.Application

case class CBACTDELCalculation(application: Application, result: CreditSumOverdue)
    extends Flow(
      "CBACTDEL-f-1",
      "CBACTDEL",
      forEachOp("CBACTDEL-l-1", "Has more credit data?", application.credit.creditBureau.creditData) { creditData =>
        rule("CBACTDEL-r-1") {
          condition("creditJoint = 1 and creditSumOverdue > 0", creditData.creditJoint == 1 && creditData.creditSumOverdue > 0) andThen {
            result.value += creditData.creditSumOverdue
          }
        }
      }
    )

final case class CreditSumOverdue(var value: BigDecimal = 0)
