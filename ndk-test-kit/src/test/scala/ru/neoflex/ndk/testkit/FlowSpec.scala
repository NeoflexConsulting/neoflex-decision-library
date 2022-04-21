package ru.neoflex.ndk.testkit

import org.scalatest.matchers.should.Matchers
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.testkit.flow._

class FlowSpec extends NdkAnyFlatSpec with Matchers {
  private val applicant =
    Applicant("LOAN1", "PROMO", Person("MEN", 29, "MARRIED", 0, "HIGHER EDUCATION", "5+", 0), Seq.empty)

  "test flow" should "resulted in underwriting level=2" in {
    val response = ApplicationResponse()
    val testFlow = TestFlow(applicant, response)

    testFlow run { flowContext =>
      flowContext table "t1" has oneExecutedRow()
      flowContext gateway "g1" has fired()
      flowContext rule "r1" has fired()
      flowContext flow "f2" has notFired()
      flowContext forEach "loop1" has notFired()
      flowContext rule "loop1-r1" has notFired()

      response.underwritingRequired should be(true)
      response.underwritingLevel should be(2)
    }
  }

  "risk level table" should "resulted in risk level=2" in {
    val response  = ApplicationResponse()
    val testTable = RiskLevelTable(applicant, response)

    testTable run { flowContext =>
      flowContext table "t1" has oneExecutedRow()
      response.riskLevel should be(2)
    }
  }

  "test flow" should "run with replaced operator" in {
    val response = ApplicationResponse()
    val testFlow = TestFlow(applicant, response) withOperators (
      "g1" -> gateway("g1") {
        when(response.riskLevel == 1) andThen {
          println("Level 1 worked")
        } and when(response.riskLevel == 2) andThen {
          println("Level 2 worked")
        } otherwise {
          println("Otherwise worked")
        }
      }
    )

    testFlow run { flowContext =>
      flowContext table "t1" has oneExecutedRow()
      flowContext gateway "g1" has fired()
      flowContext rule "r1" has notFired()
    }
  }
}
