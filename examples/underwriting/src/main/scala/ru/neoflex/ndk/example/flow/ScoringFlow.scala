package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
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
      )
    )
