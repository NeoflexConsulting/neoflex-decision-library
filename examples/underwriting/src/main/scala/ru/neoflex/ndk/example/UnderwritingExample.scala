package ru.neoflex.ndk.example

import ru.neoflex.ndk.FlowRunnerApp
import ru.neoflex.ndk.example.domain.{ Applicant, ApplicationResponse, Person }
import ru.neoflex.ndk.example.flow.UnderwritingFlow

object UnderwritingExample extends FlowRunnerApp {
  val response  = ApplicationResponse()
  val applicant = Applicant("LOAN2", "STREET", Person("MEN", 29, "MARRIED", 0, "HIGHER EDUCATION", "5+", 2))
  val flow      = UnderwritingFlow(applicant, response)

  run(flow)

  println(response)
}
