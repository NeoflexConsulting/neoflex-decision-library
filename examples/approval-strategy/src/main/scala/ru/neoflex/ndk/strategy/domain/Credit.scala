package ru.neoflex.ndk.strategy.domain

import java.time.LocalDate

final case class Credit(creditBureau: CreditBureau)
final case class CreditBureau(creditData: Seq[CreditData])
final case class CreditData(
  creditJoint: Int,
  creditDate: LocalDate,
  creditDayOverdue: Int,
  creditSumOverdue: BigDecimal)
