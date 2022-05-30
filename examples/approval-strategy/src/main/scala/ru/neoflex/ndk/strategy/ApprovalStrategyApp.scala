package ru.neoflex.ndk.strategy

import ru.neoflex.ndk.FlowRunnerApp
import ru.neoflex.ndk.strategy.domain._
import ru.neoflex.ndk.dsl.ImplicitConversions.{ charToOption, intToOption }
import ru.neoflex.ndk.strategy.domain.result.ScoringResult
import ru.neoflex.ndk.strategy.flow.ApprovalStrategyFlow

import java.time.{ LocalDate, LocalDateTime }

object ApprovalStrategyApp extends FlowRunnerApp {
  val address = Address(77, None, "Москва")
  val person  = Person(43498797, 1, 1, LocalDate.parse("1990-02-20"), '3', address)

  val applicantData = ApplicantData(
    person,
    PreviousApplications(
      Some(LocalDateTime.parse("2021-09-27T13:36:14")),
      Seq.empty
    )
  )
  val products = Seq(
    Product("PF_CL_STND", "SC"),
    Product("PF_CL_MICROCASH", "SC"),
    Product("PF_CC_HOMER_POLZA", "RD")
  )

  val creditData  = Seq(CreditData(0, 0))
  val credit      = Credit(CreditBureau(creditData))
  val application = Application(applicantData, SalesPoint(products), LocalDateTime.now(), credit)
  val result      = ScoringResult()

  val flow = ApprovalStrategyFlow(application, result)

  run(flow)

  println(result)
}
