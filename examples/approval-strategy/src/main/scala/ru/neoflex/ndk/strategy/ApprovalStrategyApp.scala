package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.dsl.ImplicitConversions.{ charToOption, intToOption }
import ru.neoflex.ndk.strategy.domain._
import ru.neoflex.ndk.strategy.domain.result.ScoringResult
import ru.neoflex.ndk.strategy.flow.ApprovalStrategyFlow
import ru.neoflex.ndk.strategy.tracking.JsonFileTrackingRunner

import java.time.{ LocalDate, LocalDateTime }

object ApprovalStrategyApp extends JsonFileTrackingRunner with App {
  val address = Address(77, None, "Москва")
  val person =
    Person(43498797, 1, 1, LocalDate.parse("1990-02-20"), '3', address)

  val applicantData = ApplicantData(
    PreviousApplications(
      Seq(
        PreviousAppPerson(43498797, Some(LocalDateTime.parse("2021-09-27T13:36:14"))
        )
      )
    )
  )
  val products = Seq(
    Product("PF_CL_STND", "SC"),
    Product("PF_CL_MICROCASH", "SC"),
    Product("PF_CC_HOMER_POLZA", "RD")
  )

  val creditData  = Seq(CreditData(0, LocalDate.now(), 0, 0))
  val credit      = Credit(CreditBureau(creditData))
  val application = Application(Seq(person), applicantData, SalesPoint(products), LocalDateTime.now(), credit)
  val result      = ScoringResult()

  val flow = ApprovalStrategyFlow(application, result)

  run(flow)

  println(result)
}
