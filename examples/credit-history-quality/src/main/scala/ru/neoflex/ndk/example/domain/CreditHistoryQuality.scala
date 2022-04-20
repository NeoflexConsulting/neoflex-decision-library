package ru.neoflex.ndk.example.domain

import java.math.BigDecimal.ZERO

final case class CreditHistoryQuality(
  var totalSumCRE: BigDecimal = ZERO,
  var isBadDebtCRE: Char = 'N',
  var goodExternalCreditHistory: Char = 'N',
  var maxDelinquencyOverallHistory: Char = '-',
  var ratioPastDueOverTotalAmount: BigDecimal = ZERO,
  var amountOfPayment: BigDecimal = ZERO) {

  override def toString: String =
    s"""Credit history quality {
    IsBadDebtCRE = $isBadDebtCRE
    Good external credit history = $goodExternalCreditHistory
    Max delinquency overall history = $maxDelinquencyOverallHistory
    Ratio past due over total amount = $ratioPastDueOverTotalAmount
    Amount of payment = $amountOfPayment\n}"""
}
