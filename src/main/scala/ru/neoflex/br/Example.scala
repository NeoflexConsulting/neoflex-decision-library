package ru.neoflex.br

import ru.neoflex.br.dsl.Flow
import ru.neoflex.br.dsl.syntax._

object Example {

  val simpleRule = rule("Simple rule") {
    condition(1 == 1 && "".isEmpty) andThen {
      println("Simple rule: started")
    }
  }

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
      }
    )
  ))
}

final case class Person(age: Int, income: Int)
final case class ScoringResult(var value: Int)
