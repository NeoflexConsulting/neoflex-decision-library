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
    application.applicantData.person.education.contains(education)
  }

  def applicantRegion(application: Application): String = {
    val address = application.applicantData.person.registeredAddress
    address.region.map(_.toString).orElse(address.regionName).getOrElse("N/A")
  }

  def applicantCity(application: Application): String = application.applicantData.person.registeredAddress.town
}
