package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.catsSyntaxOptionId
import org.scalatest.{ EitherValues, Inside }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.error.{ DictionaryFieldTypeMismatch, DictionaryLoadingError }

class SeparateFieldsDictSpec extends AnyFlatSpec with Matchers with EitherValues with Inside {
  "a dictionary" should "return specified value field" in {
    val result = CurrencyNumericFieldDict.getUnsafe("name" like "Yen")
    result should be(392.some)
  }

  "a dictionary with wrong value field type defined" should "fail to load" in {
    val result = ValueFieldTypeMismatchDict("alpha" is "RUB").get
    val error  = result.left.value
    inside(error) {
      case DictionaryFieldTypeMismatch(fieldName, _, _) =>
        fieldName should be("numeric".some)
    }
  }

  "a dictionary without value field" should "fail to load" in {
    val error = AbsenceValueFieldDict("code" is "123").get.left.value
    inside(error) {
      case DictionaryLoadingError(dictionaryName, message, _) =>
        dictionaryName should be("absence_value_field_dict")
        message should be("Without valueField definition, each dictionary record should have only two fields")
    }
  }

  "a kv dictionary without valueField in yaml" should "be loaded and return result" in {
    val result = CurrencyAutoValueField.getUnsafe("alpha" is "RUB")
    result should be (643.some)
  }
}

private object CurrencyNumericFieldDict extends ProductIndexedDictionary[Currency, Int]("currency_numeric_field_dict")
private object ValueFieldTypeMismatchDict
    extends ProductIndexedDictionary[Currency, String]("currency_numeric_field_dict", eagerLoad = false)
private object AbsenceValueFieldDict  extends MapIndexedDictionary[Int]("absence_value_field_dict", eagerLoad = false)
private object CurrencyAutoValueField extends ProductIndexedDictionary[CurrencyShort, Int]("currency_auto_value_field")
