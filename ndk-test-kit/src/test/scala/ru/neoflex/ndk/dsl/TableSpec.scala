package ru.neoflex.ndk.dsl

import org.scalatest.matchers.should.Matchers.{ be, convertToAnyShouldWrapper }
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.testkit.NdkAnyFlatSpec

class TableSpec extends NdkAnyFlatSpec {
  "empty on string" should "work" in {
    assertTable(shouldEmpty = true, "")
  }

  "nonEmpty on string" should "work" in {
    assertTable(shouldEmpty = false, "asd")
  }

  "empty on seq" should "work" in {
    assertTable(shouldEmpty = true, Seq.empty)
  }

  "nonEmpty on seq" should "work" in {
    assertTable(shouldEmpty = false, Seq(1, 2, 3))
  }

  private def assertTable[T](shouldEmpty: Boolean, expValue: T): Unit = {
    var isEmpty = false
    val t = table("t1") {
      expressions(
        "value" expr expValue
      ) andConditions (
        row(empty()).apply { isEmpty = true },
        row(nonEmpty()).apply { isEmpty = false }
      )
    }

    t run { context =>
      context table "t1" has oneExecutedRow()
      isEmpty should be(shouldEmpty)
    }
  }
}
