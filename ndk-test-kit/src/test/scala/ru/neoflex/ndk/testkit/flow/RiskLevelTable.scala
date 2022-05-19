package ru.neoflex.ndk.testkit.flow

import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._

case class RiskLevelTable(in: Applicant, out: ApplicationResponse)
    extends Table(
      "t1",
      "Risk level table",
      expressions(
        "role" expr in.role,
        "channel" expr in.channel
      ) andConditions (
        row(eqv("APPLICANT"), eqv("RECOMMEND")).apply { out.riskLevel = 2 },
        row(eqv("APPLICANT"), eqv("PROMO")).apply { out.riskLevel = 3 },
        row(eqv("APPLICANT"), eqv("STREET")).apply { out.riskLevel = 3 },
        row(eqv("LOAN1"), eqv("RECOMMEND")).apply { out.riskLevel = 1 },
        row(eqv("LOAN1"), eqv("PROMO")).apply { out.riskLevel = 2 },
        row(eqv("LOAN1"), eqv("STREET")).apply { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("RECOMMEND")).apply { out.riskLevel = 1 },
        row(eqv("LOAN2"), eqv("PROMO")).apply { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("STREET")).apply { out.riskLevel = 2 },
        row(eqv("LOAN3"), eqv("RECOMMEND")).apply { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("PROMO")).apply { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("STREET")).apply { out.riskLevel = 1 },
      )
    )
