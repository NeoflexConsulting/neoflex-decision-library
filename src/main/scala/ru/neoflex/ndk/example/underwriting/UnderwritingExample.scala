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
              when { in.age < 39 } run {
                out.scoring += 13
              } and when { in.age >= 30 && in.age <= 39 } run {
                out.scoring += 17
              } and when { in.age >= 40 && in.age <= 49 } run {
                out.scoring += 23
              } otherwise {
                out.scoring += 23
              }
            },

            gateway("maritalStatus") {
              when { in.maritalStatus == "MARRIED" } run {
                out.scoring += 27
              } and when { in.maritalStatus == "REMARRIAGE" } run {
                out.scoring += 22
              } and when { in.maritalStatus == "WIDOWHOOD" } run {
                out.scoring += 20
              } and when { in.maritalStatus == "DIVORCED" } run {
                out.scoring += 10
              } otherwise {
                out.scoring += 0
              }
            },

            gateway("childrenQty") {
              when { in.childrenQty == 0 } run {
                out.scoring += 0
              } and when { in.childrenQty == 1 } run {
                out.scoring += 6
              } and when { in.childrenQty == 2 } run {
                out.scoring += 13
              } and when { in.childrenQty == 3 } run {
                out.scoring += 6
              } otherwise {
                out.scoring += 0
              }
            },

            gateway("education") {
              when { in.education == "BASIC GENERAL" } run {
                out.scoring += 0
              } and when { in.education == "SECONDARY GENERAL" } run {
                out.scoring += 0
              } and when { in.education == "PRIMARY PROFESSIONAL" } run {
                out.scoring += 2
              } and when { in.education == "SECONDARY PROFESSIONAL" } run {
                out.scoring += 2
              } and when { in.education == "INCOMPLETE HIGHER EDUCATION" } run {
                out.scoring += 7
              } and when { in.education == "HIGHER EDUCATION" } run {
                out.scoring += 10
              } and when { in.education == "MORE THAN ONE HIGHER EDUCATION" } run {
                out.scoring += 15
              } otherwise {
                out.scoring += 15
              }
            },

            gateway("workExperience") {
              when { in.workExperience == "3-" } run {
                out.scoring += 0
              } and when { in.workExperience == "3-5" } run {
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
            when("Level 1") { out.riskLevel == 1 } run {
              out.underwritingRequired = false
            } and when("Level 3") { out.riskLevel == 3 } run {
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
