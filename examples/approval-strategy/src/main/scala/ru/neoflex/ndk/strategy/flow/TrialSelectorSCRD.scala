package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.{ ScoringResult, Trial, WFDetails }

final case class TrialSelectorSCRD(application: Application, result: ScoringResult)
    extends Table(
      "ts-t-1",
      "Trial selector SCRD",
      expressions(
        "activeScOffer" expr application.person.activeScOffer,
        "productFamilies" expr application.salesPoint.products.map(_.productFamily)
      ) andConditions (
        row(eqv(1), contains("PF_CL_STND")).apply("Add TR_CL_STND") {
          result.trials :+= Trial("TR_CL_STND", WFDetails("CONTINUE"))
        },
        row(any(), contains("PF_CC_HOMER_POLZA")).apply("Add TR_CC_HOMER_POLZA_STND") {
          result.trials :+= Trial("TR_CC_HOMER_POLZA_STND", WFDetails("CONTINUE"))
        },
        row(any(), contains("PF_CC_TW_LG")).apply("Add TR_CC_TW_LG_STND") {
          result.trials :+= Trial("TR_CC_TW_LG_STND", WFDetails("CONTINUE"))
        }
      )
    )
