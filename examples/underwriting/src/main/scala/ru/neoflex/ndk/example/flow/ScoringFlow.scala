package ru.neoflex.ndk.example.flow

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.example.domain.Person
import ru.neoflex.ndk.example.domain.ApplicationResponse
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption

case class ScoringFlow(in: Person, out: ApplicationResponse)
    extends Flow(
      "psc-f-1",
      "Person scoring calculation flow",
      flowOps(
        rule("s-r-1") {
          condition("sex = WOMAN", in.sex == "WOMAN") andThen {
            out.scoring += 50
          } otherwise {
            out.scoring -= 10
          }
        },

        rule("a-r-1") {
          condition("age < 39", in.age < 39) andThen {
            out.scoring += 13
          } condition("age >= 30 and age <= 39", in.age >= 30 && in.age <= 39) andThen {
            out.scoring += 17
          } condition("age >= 40 and age <= 49", in.age >= 40 && in.age <= 49) andThen {
            out.scoring += 23
          } otherwise {
            out.scoring += 23
          }
        },

        rule("ms-r-1") {
          condition("Is married?", in.maritalStatus == "MARRIED") andThen {
            out.scoring += 27
          } condition("Is there remarriage?", in.maritalStatus == "REMARRIAGE") andThen {
            out.scoring += 22
          } condition("Is there widowhood?", in.maritalStatus == "WIDOWHOOD") andThen {
            out.scoring += 20
          } condition("Is there divorce?", in.maritalStatus == "DIVORCED") andThen {
            out.scoring += 10
          } otherwise {
            out.scoring += 0
          }
        },

        rule("cq-r-1") {
          condition("No children?", in.childrenQty == 0) andThen {
            out.scoring += 0
          } condition("One child?", in.childrenQty == 1) andThen {
            out.scoring += 6
          } condition("Two children?", in.childrenQty == 2) andThen {
            out.scoring += 13
          } condition("Three children?", in.childrenQty == 3) andThen {
            out.scoring += 6
          } otherwise {
            out.scoring += 0
          }
        },

        rule("edu-r-1") {
          condition("Basic education?", in.education == "BASIC GENERAL") andThen {
            out.scoring += 0
          } condition("General secondary education?", in.education == "SECONDARY GENERAL") andThen {
            out.scoring += 0
          } condition("Primary professional education?", in.education == "PRIMARY PROFESSIONAL") andThen {
            out.scoring += 2
          } condition("Secondary professional education?", in.education == "SECONDARY PROFESSIONAL") andThen {
            out.scoring += 2
          } condition("Incomplete higher education?", in.education == "INCOMPLETE HIGHER EDUCATION") andThen {
            out.scoring += 7
          } condition("Higher education?", in.education == "HIGHER EDUCATION") andThen {
            out.scoring += 10
          } condition("More than one higher education?", in.education == "MORE THAN ONE HIGHER EDUCATION") andThen {
            out.scoring += 15
          } otherwise {
            out.scoring += 15
          }
        },

        rule("wexp-r-1") {
          condition("Work experience < 3 years?", in.workExperience == "3-") andThen {
            out.scoring += 0
          } condition("Work experience from 3 to 5 years?", in.workExperience == "3-5") andThen {
            out.scoring += 6
          } otherwise {
            out.scoring += 15
          }
        },

        rule("lnsq-r-1") {
          condition("Had any loans?", in.loansQty >= 1) andThen {
            out.scoring += 15
          }
        }
      )
    )
