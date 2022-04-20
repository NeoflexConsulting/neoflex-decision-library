package ru.neoflex.ndk.example.domain

import java.math.BigDecimal.ZERO

case class IntermediateValues(
  var maxCurrentDelq: Int = 0,
  var currentDelqSum: BigDecimal = ZERO,
  var totalCreditLimit: BigDecimal = ZERO,
  var totalDelqBalance: BigDecimal = ZERO,
  var totalSumCRE: BigDecimal = ZERO,
  var amountOfPayment: BigDecimal = ZERO)
