package ru.neoflex.ndk.strategy.domain

import java.time.LocalDate

final case class Person(
  cuid: Int,
  activeRdOffer: Int,
  activeScOffer: Int,
  birth: LocalDate,
  education: Option[Char],
  registeredAddress: Address)

final case class Address(region: Option[Int], regionName: Option[String], townCode: String)
