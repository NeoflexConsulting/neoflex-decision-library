package ru.neoflex.ndk.example.domain

final case class Loan(
  totalOutstanding: BigDecimal,
  totalOutstandingCurCode: String,
  paymentDiscipline: Seq[Char],
  loanState: Int,
  currentDelq: Int,
  delqBalance: BigDecimal,
  delqBalanceCurCode: String,
  creditLimit: BigDecimal,
  creditLimitCurCode: String,
  maxPayment: BigDecimal)
