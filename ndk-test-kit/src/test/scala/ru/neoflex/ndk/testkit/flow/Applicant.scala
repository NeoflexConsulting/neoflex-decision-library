package ru.neoflex.ndk.testkit.flow

final case class Applicant(role: String, channel: String, person: Person, loans: Seq[Loan])
final case class Person(
  sex: String,
  age: Int,
  maritalStatus: String,
  childrenQty: Int,
  education: String,
  workExperience: String,
  loansQty: Int)

final case class Loan(totalOutstanding: BigDecimal)
