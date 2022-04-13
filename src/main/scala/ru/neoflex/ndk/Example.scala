package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.{Flow, Rule, Table}
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._

object Example {

  val simpleRule: Rule = rule("Simple rule") {
    condition(1 == 1 && "".isEmpty) andThen {
      println("Simple rule: started")
    }
  }

  case class SimpleTable(in: Person) extends
    Table("Simple table",
      expressions(
        "income" expr { in.income },
        "age" expr { in.age }
      ) withActions (
        "printIncomeAge" action { (income, age) =>
          println(s"Income and age: $income, $age")
        },
        "printIncome" action { income =>
          println(s"Income: $income")
        }
      ) andConditions (
        row(any(), eqv(20)) apply { "printIncome" withArgs in.income },
        row(any(), gt(26)) apply { "printIncomeAge" withArgs(in.income, in.age) },
        row(any(), any()) apply { println(s"Person is: $in") }
      ))

  case class ScoringFlow(in: Person, out: ScoringResult) extends Flow("Scoring flow", flow(
    simpleRule,

    rule("Nested rule") {
      condition(in.age > 25) andThen {
        out.value += 10
      } otherwise {
        out.value += 5
      }
    },

    flow("Nested flow")(
      gateway("Income level based scoring") {
        when("200000") {
          in.income > 200000
        } run {
          out.value += 42
        } and when("100000") {
          in.income > 100000
        } run {
          out.value += 27
        } and when("50000") {
          in.income > 50000
        } run {
          out.value += 5
        } otherwise {
          out.value -= 1
        }
      },

      rule("Ending rule") {
        condition(out.value > 30) andThen {
          println("Good result")
        } otherwise {
          println("Bad result")
        }
      },

      table("Nested table") {
        expressions (
          "intValue" expr 1,
          "stringValue" expr "string"
        ) andConditions (
          row(eqv(1), empty()) { println("eqv(1), empty") },
          row(eqv(1), eqv("string")) { println("eqv(1), eqv(string)") }
        )
      }
    ),

    SimpleTable(in)
  ))
}

final case class Person(age: Int, income: Int)
final case class ScoringResult(var value: Int)
