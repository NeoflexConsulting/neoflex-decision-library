package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.{ IntermediateValues, Loan }

case class ValuesPrecalculating(loans: Seq[Loan], values: IntermediateValues)
    extends Flow("Common values precalculating flow", forEach(NoId, Some("Has more values for precalculation?"), loans) { loan =>
      values.totalSumCRE += CurrenciesDict.toRub(loan.totalOutstanding, loan.totalOutstandingCurCode)
      values.totalDelqBalance += CurrenciesDict.toRub(loan.delqBalance, loan.delqBalanceCurCode)
      values.totalCreditLimit += CurrenciesDict.toRub(loan.creditLimit, loan.creditLimitCurCode)
      values.maxCurrentDelq = math.max(values.maxCurrentDelq, loan.currentDelq)
      values.currentDelqSum += CurrenciesDict.toRub(loan.delqBalance, loan.delqBalanceCurCode)
      values.amountOfPayment += loan.maxPayment
    })
