package ru.neoflex.ndk.strategy.domain

import java.time.LocalDateTime

final case class Application(
  persons: Seq[Person],
  applicantData: ApplicantData,
  salesPoint: SalesPoint,
  sysdate: LocalDateTime,
  credit: Credit) {
  def person: Person = persons.head
}

final case class ApplicantData(previousApplications: PreviousApplications)
final case class PreviousApplications(persons: Seq[PreviousAppPerson]) {
  def person: PreviousAppPerson = persons.head
}

final case class PreviousAppPerson(cuid: Int, firstaDate: Option[LocalDateTime])
