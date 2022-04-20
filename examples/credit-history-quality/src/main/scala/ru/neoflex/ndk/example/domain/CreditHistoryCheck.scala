package ru.neoflex.ndk.example.domain

final case class CreditHistoryCheck(loans: Seq[Loan], loansOverview: LoansOverview)
final case class LoansOverview(worstStatusEver: Char)
