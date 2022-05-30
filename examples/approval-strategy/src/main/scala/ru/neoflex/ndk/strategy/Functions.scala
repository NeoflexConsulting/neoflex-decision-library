package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.strategy.domain.Application

import java.time.Period

object Functions {
  def isNewClient(application: Application): Boolean = {
    application.applicantData.previousApplications.firstaDate.forall { firstDate =>
      val monthsBetweenApplications =
        Math.abs(Period.between(application.sysdate.toLocalDate, firstDate.toLocalDate).toTotalMonths)
      monthsBetweenApplications < 1
    }
  }

  def getPersonAge(application: Application): Int = {
    val age =
      Period.between(application.applicantData.person.birthDate, application.sysdate.toLocalDate).normalized()
    Math.abs(age.getYears)
  }

  def hasEducation(application: Application, education: Char): Boolean = {
    application.applicantData.person.education.contains(education)
  }

  def isInRegion(application: Application, region: Int): Boolean = {
    val address = application.applicantData.person.registeredAddress
    address.region
      .map(_.toString)
      .orElse(address.regionName)
      .flatMap { region =>
        RegionRiskGradeTable.getRisk(region, address.town)
      }
      .contains(region)
  }
}

object RegionRiskGradeTable {
  private val regions = List(
    Region("80", "N/A", 3),
    Region("%агинский%бурятский%", "N/A", 3),
    Region("75", "N/A", 3),
    Region("%забайкальский%", "N/A", 3),
    Region("85", "N/A", 3),
    Region("%иркутская%ордынский%", "N/A", 3),
    Region("38", "%братск%", 2),
    Region("%иркутская%", "%братск%", 2),
    Region("38", "N/A", 3),
    Region("42", "N/A", 3),
    Region("%кемеровская%", "N/A", 3),
    Region("24", "%норильск%", 1),
    Region("%красноярский%", "%норильск%", 1),
    Region("24", "%красноярск%", 2),
    Region("%красноярский%", "%красноярск%", 2),
    Region("24", "N/A", 3),
    Region("%красноярский%", "N/A", 3)
  )

  def getRisk(region: String, city: String): Option[Int] = {
    regions
      .find(r => (r.code contains region.toLowerCase()) && (r.city contains city.toLowerCase()))
      .orElse(regions.find(r => r.code == region))
      .map(_.result)
  }

  final case class Region(code: String, city: String, result: Int)
}
