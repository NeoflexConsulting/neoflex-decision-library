package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.strategy.domain.Application

object Functions {
  def hasEducation(application: Application, education: Char): Boolean = {
    application.applicantData.person.education.contains(education)
  }

  def applicantRegion(application: Application): String = {
    val address = application.applicantData.person.registeredAddress
    address.region.map(_.toString).orElse(address.regionName).getOrElse("N/A")
  }

  def applicantCity(application: Application): String = application.applicantData.person.registeredAddress.town
}
