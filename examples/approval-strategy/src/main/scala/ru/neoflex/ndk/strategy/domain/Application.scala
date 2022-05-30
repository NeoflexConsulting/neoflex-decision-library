package ru.neoflex.ndk.strategy.domain

import java.time.LocalDateTime

final case class Application(
  applicantData: ApplicantData,
  salesPoint: SalesPoint,
  sysdate: LocalDateTime,
  credit: Credit)

final case class ApplicantData(person: Person, previousApplications: PreviousApplications)

final case class PreviousApplications(firstaDate: Option[LocalDateTime], persons: Seq[Person])
