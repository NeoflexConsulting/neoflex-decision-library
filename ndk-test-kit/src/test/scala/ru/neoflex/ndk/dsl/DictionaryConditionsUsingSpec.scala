package ru.neoflex.ndk.dsl

import org.scalatest.matchers.should.Matchers.{ be, convertToAnyShouldWrapper }
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.testkit.NdkAnyFlatSpec

import java.util.concurrent.atomic.AtomicInteger

class DictionaryConditionsUsingSpec extends NdkAnyFlatSpec {
  "all rows" should "be executed" in {
    val sampleTable = table("table-spec-1") {
      expressions(
        "Novosibirsk" expr RegionRiskTable("54"),
        "Moscow" expr RegionRiskTable("77"),
        "Saint-Petersburg" expr RegionRiskTable("78"),
        "Omsk" expr RegionRiskTable("55")
      ) andConditions (
        row(contains(3), any(), any(), any()).apply {},
        row(any(), contains(5), any(), any()).apply {},
        row(any(), any(), contains(1), any()).apply {},
        row(any(), any(), any(), empty()).apply {}
      )
    }

    sampleTable run { ctx =>
      ctx table sampleTable.id has fired()
      ctx table sampleTable.id has executedRows(4)
    }
  }

  "only matched rows" should "be executed" in {
    val executedRow = new AtomicInteger()
    val t = table("table-spec-2") {
      expressions(
        "Novosibirsk" expr RegionRiskTable("54"),
        "Saint-Petersburg" expr RegionRiskTable("78")
      ) andConditions (
        row(contains(3), contains(1)).apply {
          executedRow.set(1)
        },
        row(contains(111), contains(123)).apply {
          executedRow.set(2)
        }
      )
    }

    t run { ctx =>
      ctx table t.id has oneExecutedRow()
      executedRow.get() should be(1)
    }
  }

  "matched condition" should "be executed" in {
    val firedCondition = new AtomicInteger()
    val dictionaryRule = rule("dict-rule-spec-1") {
      condition(RegionRiskTable("54") == 1) andThen {
        firedCondition.set(1)
      } condition (RegionRiskTable("54") == 3) andThen {
        firedCondition.set(2)
      } otherwise {
        firedCondition.set(3)
      }
    }

    dictionaryRule run { ctx =>
      ctx rule dictionaryRule.id has fired()
      firedCondition.get() should be(2)
    }
  }

  "matched branches" should "be executed" in {
    val firedCondition = new AtomicInteger()
    val dictGateway = gateway("dict-gateway-spec-1") {
      when(RegionRiskTable("54") == 1) andThen action {
        firedCondition.set(1)
      } and when(RegionRiskTable("54") == 3) andThen action {
        firedCondition.set(2)
      } otherwise action {
        firedCondition.set(3)
      }
    }

    dictGateway run { ctx =>
      ctx gateway dictGateway.id has fired()
      firedCondition.get() should be (2)
    }
  }
}
