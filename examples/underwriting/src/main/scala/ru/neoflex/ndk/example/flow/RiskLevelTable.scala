package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.example.domain.{ Applicant, ApplicationResponse }
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption

case class RiskLevelTable(in: Applicant, out: ApplicationResponse)
    extends Table(
      "rl-t-1",
      "Risk level table",
      expressions (
        "role" expr in.role,
        "channel" expr in.channel
      ) andConditions (
        row(eqv("APPLICANT"), eqv("RECOMMEND")).apply { out.riskLevel = 2 },
        row(eqv("APPLICANT"), eqv("RECOMMEND")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("APPLICANT"), eqv("PROMO")).apply("riskLevel = 3") { out.riskLevel = 3 },
        row(eqv("APPLICANT"), eqv("STREET")).apply("riskLevel = 3") { out.riskLevel = 3 },
        row(eqv("LOAN1"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN1"), eqv("PROMO")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN1"), eqv("STREET")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN2"), eqv("PROMO")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("STREET")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN3"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("PROMO")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("STREET")).apply("riskLevel = 1") { out.riskLevel = 1 },
      )
    )
