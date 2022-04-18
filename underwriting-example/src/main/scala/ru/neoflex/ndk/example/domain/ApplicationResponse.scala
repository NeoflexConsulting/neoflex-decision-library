package ru.neoflex.ndk.example.domain

final case class ApplicationResponse(
  var riskLevel: Int = 0,
  var scoring: Int = 100,
  var underwritingRequired: Boolean = false,
  var underwritingLevel: Int = -1)
