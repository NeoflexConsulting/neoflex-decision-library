package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.example.domain.{ Applicant, ApplicationResponse }

case class RiskLevelTable(in: Applicant, out: ApplicationResponse)
    extends Table(
      "Risk level table",
      expressions (
        "role" expr in.role,
        "channel" expr in.channel
      ) andConditions (
        row(eqv("APPLICANT"), eqv("RECOMMEND")) { out.riskLevel = 2 },
        row(eqv("APPLICANT"), eqv("PROMO")) { out.riskLevel = 3 },
        row(eqv("APPLICANT"), eqv("STREET")) { out.riskLevel = 3 },
        row(eqv("LOAN1"), eqv("RECOMMEND")) { out.riskLevel = 1 },
        row(eqv("LOAN1"), eqv("PROMO")) { out.riskLevel = 2 },
        row(eqv("LOAN1"), eqv("STREET")) { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("RECOMMEND")) { out.riskLevel = 1 },
        row(eqv("LOAN2"), eqv("PROMO")) { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("STREET")) { out.riskLevel = 2 },
        row(eqv("LOAN3"), eqv("RECOMMEND")) { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("PROMO")) { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("STREET")) { out.riskLevel = 1 },
      )
    )
