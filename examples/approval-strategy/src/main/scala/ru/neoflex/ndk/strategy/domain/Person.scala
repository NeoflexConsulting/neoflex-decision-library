package ru.neoflex.ndk.strategy.domain

import java.time.LocalDate

final case class Person(
  cuid: Int,
  activeRdOffer: Int,
  activeScOffer: Int,
  birthDate: LocalDate,
  education: Option[Char],
  registeredAddress: Address)

final case class Address(region: Option[Int], regionName: Option[String], town: String)
