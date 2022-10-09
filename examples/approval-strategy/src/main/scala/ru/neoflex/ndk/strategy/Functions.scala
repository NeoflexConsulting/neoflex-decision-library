package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.strategy.domain.Application

object Functions {
  def maxOverdueDaysLast24Months(application: Application): Int = {
    val minDate = application.sysdate.toLocalDate.minusMonths(24)
    application.credit.creditBureau.creditData
      .filter(d => !d.creditDate.isBefore(minDate))
      .map(_.creditDayOverdue)
      .maxOption
      .getOrElse(0)
  }

  def maxCurrentSumOverdue(application: Application): BigDecimal = {
    application.credit.creditBureau.creditData
      .map(_.creditSumOverdue)
      .maxOption
      .getOrElse(0)
  }

  def hasEducation(application: Application, education: Char): Boolean = {
    application.person.flatMap(_.education).contains(education)
  }

  def applicantRegion(application: Application): String = {
    val address = application.person.map(_.registeredAddress)
    address.flatMap(_.region.map(_.toString)).orElse(address.flatMap(_.regionName)).getOrElse("N/A")
  }

  def applicantCity(application: Application): String = application.person.map(_.registeredAddress.townCode).getOrElse("")
}
