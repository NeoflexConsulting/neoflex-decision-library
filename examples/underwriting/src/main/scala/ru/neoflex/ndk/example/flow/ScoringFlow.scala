package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.Person
import ru.neoflex.ndk.example.domain.ApplicationResponse

case class ScoringFlow(in: Person, out: ApplicationResponse)
    extends Flow(
      "Person scoring calculation flow",
      flowOps(
        rule("sex") {
          condition(in.sex == "WOMAN") andThen {
            out.scoring += 50
          } otherwise {
            out.scoring -= 10
          }
        },

        rule("age") {
          condition(in.age < 39) andThen {
            out.scoring += 13
          } condition(in.age >= 30 && in.age <= 39) andThen {
            out.scoring += 17
          } condition(in.age >= 40 && in.age <= 49) andThen {
            out.scoring += 23
          } otherwise {
            out.scoring += 23
          }
        },

        rule("maritalStatus") {
          condition(in.maritalStatus == "MARRIED") andThen {
            out.scoring += 27
          } condition(in.maritalStatus == "REMARRIAGE") andThen {
            out.scoring += 22
          } condition(in.maritalStatus == "WIDOWHOOD") andThen {
            out.scoring += 20
          } condition(in.maritalStatus == "DIVORCED") andThen {
            out.scoring += 10
          } otherwise {
            out.scoring += 0
          }
        },

        rule("childrenQty") {
          condition(in.childrenQty == 0) andThen {
            out.scoring += 0
          } condition(in.childrenQty == 1) andThen {
            out.scoring += 6
          } condition(in.childrenQty == 2) andThen {
            out.scoring += 13
          } condition(in.childrenQty == 3) andThen {
            out.scoring += 6
          } otherwise {
            out.scoring += 0
          }
        },

        rule("education") {
          condition(in.education == "BASIC GENERAL") andThen {
            out.scoring += 0
          } condition(in.education == "SECONDARY GENERAL") andThen {
            out.scoring += 0
          } condition(in.education == "PRIMARY PROFESSIONAL") andThen {
            out.scoring += 2
          } condition(in.education == "SECONDARY PROFESSIONAL") andThen {
            out.scoring += 2
          } condition(in.education == "INCOMPLETE HIGHER EDUCATION") andThen {
            out.scoring += 7
          } condition(in.education == "HIGHER EDUCATION") andThen {
            out.scoring += 10
          } condition(in.education == "MORE THAN ONE HIGHER EDUCATION") andThen {
            out.scoring += 15
          } otherwise {
            out.scoring += 15
          }
        },

        rule("workExperience") {
          condition(in.workExperience == "3-") andThen {
            out.scoring += 0
          } condition(in.workExperience == "3-5") andThen {
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
      )
    )
