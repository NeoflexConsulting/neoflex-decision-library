package ru.neoflex.ndk.example.flow

object CurrenciesDict {
  private val rates = Map("USD" -> 79, "EUR" -> 89, "RUR" -> 1)

  def toRub(sum: BigDecimal, curCode: String): BigDecimal = rates(curCode) * sum
}
