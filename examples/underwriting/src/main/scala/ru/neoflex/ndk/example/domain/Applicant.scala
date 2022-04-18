package ru.neoflex.ndk.example.domain

final case class Applicant(role: String, channel: String, person: Person)
final case class Person(
  sex: String,
  age: Int,
  maritalStatus: String,
  childrenQty: Int,
  education: String,
  workExperience: String,
  loansQty: Int)
