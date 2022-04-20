package ru.neoflex.ndk.example

import ru.neoflex.ndk.FlowRunnerApp
import ru.neoflex.ndk.example.domain.{Applicant, CreditHistoryCheck, CreditHistoryQuality, Loan, LoansOverview, Success}
import ru.neoflex.ndk.example.flow.QualityFlow

object ExampleApp extends FlowRunnerApp {

  val loan1 = Loan(
    totalOutstanding = 3000000,
    totalOutstandingCurCode = "RUR",
    paymentDiscipline = "011A21",
    loanState = 1,
    currentDelq = 0,
    delqBalance = 0,
    delqBalanceCurCode = "RUR",
    creditLimit = 5000000,
    creditLimitCurCode = "RUR",
    maxPayment = 100000
  )
  val loan2 = Loan(
    totalOutstanding = 800000,
    totalOutstandingCurCode = "RUR",
    paymentDiscipline = "011111111",
    loanState = 1,
    currentDelq = 0,
    delqBalance = 0,
    delqBalanceCurCode = "RUR",
    creditLimit = 1000000,
    creditLimitCurCode = "RUR",
    maxPayment = 100000
  )
  val check = CreditHistoryCheck(Seq(loan1, loan2), LoansOverview('A'))
  val applicant = Applicant("LegalApplicant", check, Success)
  val quality = CreditHistoryQuality()

  val flow = QualityFlow(applicant, quality)

  run(flow)

  println(quality)
}
