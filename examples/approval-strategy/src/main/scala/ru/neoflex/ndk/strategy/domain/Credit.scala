package ru.neoflex.ndk.strategy.domain

final case class Credit(creditBureau: CreditBureau)
final case class CreditBureau(creditData: Seq[CreditData])
final case class CreditData(creditJoint: Int, creditSumOverdue: BigDecimal)
