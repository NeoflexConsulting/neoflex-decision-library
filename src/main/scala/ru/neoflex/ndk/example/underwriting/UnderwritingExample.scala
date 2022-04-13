package ru.neoflex.ndk.example.underwriting

import ru.neoflex.ndk.TestExecutor
import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._

object UnderwritingExample extends App {

  case class ScoringFlow(in: Person, out: ApplicationResponse)
      extends Flow(
        "Scoring based underwriting Flow",
        flow(
          flow("Scoring Flow")(

            rule("sex") {
              condition(in.sex == "WOMAN") andThen {
                out.scoring += 50
              } otherwise {
                out.scoring -= 10
              }
            },

            gateway("age") {
              when(in.age < 39) andThen {
                out.scoring += 13
              } and when(in.age >= 30 && in.age <= 39) andThen {
                out.scoring += 17
              } and when(in.age >= 40 && in.age <= 49) andThen {
                out.scoring += 23
              } otherwise {
                out.scoring += 23
              }
            },

            gateway("maritalStatus") {
              when(in.maritalStatus == "MARRIED") andThen {
                out.scoring += 27
              } and when(in.maritalStatus == "REMARRIAGE") andThen {
                out.scoring += 22
              } and when(in.maritalStatus == "WIDOWHOOD") andThen {
                out.scoring += 20
              } and when(in.maritalStatus == "DIVORCED") andThen {
                out.scoring += 10
              } otherwise {
                out.scoring += 0
              }
            },

            gateway("childrenQty") {
              when(in.childrenQty == 0) andThen {
                out.scoring += 0
              } and when(in.childrenQty == 1) andThen {
                out.scoring += 6
              } and when(in.childrenQty == 2) andThen {
                out.scoring += 13
              } and when(in.childrenQty == 3) andThen {
                out.scoring += 6
              } otherwise {
                out.scoring += 0
              }
            },

            gateway("education") {
              when(in.education == "BASIC GENERAL") andThen {
                out.scoring += 0
              } and when(in.education == "SECONDARY GENERAL") andThen {
                out.scoring += 0
              } and when(in.education == "PRIMARY PROFESSIONAL") andThen {
                out.scoring += 2
              } and when(in.education == "SECONDARY PROFESSIONAL") andThen {
                out.scoring += 2
              } and when(in.education == "INCOMPLETE HIGHER EDUCATION") andThen {
                out.scoring += 7
              } and when(in.education == "HIGHER EDUCATION") andThen {
                out.scoring += 10
              } and when(in.education == "MORE THAN ONE HIGHER EDUCATION") andThen {
                out.scoring += 15
              } otherwise {
                out.scoring += 15
              }
            },

            gateway("workExperience") {
              when(in.workExperience == "3-") andThen {
                out.scoring += 0
              } and when(in.workExperience == "3-5") andThen {
                out.scoring += 6
              } otherwise {
                out.scoring += 15
              }
            },

            rule("loansQty") {
              condition(in.loansQty >= 1) andThen {
                out.scoring += 15
              }
            }
          ),

          rule("Underwriting level") {
            condition(out.scoring < 150) andThen {
              out.underwritingRequired = true
              out.underwritingLevel = Some(2)
            } otherwise {
              out.underwritingRequired = true
              out.underwritingLevel = Some(1)
            }
          }
        )
      )

  case class UnderwritingFlow(in: Client, out: ApplicationResponse)
      extends Flow(
        "Underwriting flow",
        flow(

          table("Risk level table") {
            expressions(
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
          },

          gateway("Risk level gateway") {
            when("Level 1") { out.riskLevel == 1 } andThen {
              out.underwritingRequired = false
            } and when("Level 3") { out.riskLevel == 3 } andThen {
              out.underwritingRequired = true
              out.underwritingLevel = Some(2)
            } otherwise ScoringFlow(in.person, out)
          }
        )
      )

  val response = ApplicationResponse()
  TestExecutor.execute(
    UnderwritingFlow(
      Client("LOAN2", "STREET", Person("MEN", 29, "MARRIED", 0, "HIGHER EDUCATION", "5+", 3)),
      response
    )
  )
  println(response)
}

final case class Client(role: String, channel: String, person: Person)
final case class Person(
  sex: String,
  age: Int,
  maritalStatus: String,
  childrenQty: Int,
  education: String,
  workExperience: String,
  loansQty: Int)

final case class ApplicationResponse(
  var riskLevel: Int = 0,
  var scoring: Int = 100,
  var underwritingRequired: Boolean = false,
  var underwritingLevel: Option[Int] = None)
