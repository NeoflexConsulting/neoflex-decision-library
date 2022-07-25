package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ EitherValues, Inside, OptionValues }
import ru.neoflex.ndk.dsl.implicits._

class IndexedDictionarySpec extends AnyWordSpec with EitherValues with Matchers with Inside with OptionValues {
  "A condition" when {
    "eq" should {
      "return result" in {
        val lazyCondition = RegionDict("code" is "77") == "Moscow"
        lazyCondition.eval().value should be(true)
      }

      "result in empty" in {
        val result = RegionDict("code" is "9875").get.value
        result should be(None)
      }

      "return result despite the letters case" in {
        val result = PopulationsDict("country" is "ireLand").get.value
        result should be (5000000.some)
      }
    }

    "like" should {
      "return result by exact match" in {
        val result = RegionDict("code" like "77").get.value
        result should be("Moscow".some)
      }

      "return result by prefix" in {
        val lazyCondition = RegionDict("code" like "7%") == "Moscow"
        lazyCondition.eval().value should be(true)
      }

      "return result by suffix" in {
        val result = RegionDict("code" like "%7").get.value
        result should be("Moscow".some)
      }

      "return result by pattern" in {
        val result = RegionDict("code" like "%7%").get.value
        result should be("Moscow".some)
      }

      "return first result by full pattern" in {
        val result = RegionDict("code" like "%").get.value
        result should be("Novosibirsk".some)
      }

      "result in empty" in {
        val result = RegionDict("code" like "nonexistent").get.value
        result should be(None)
      }

      "return result despite the letters case" in {
        val result = PopulationsDict("country" like "pER%").get.value
        result should be (34000000.some)
      }
    }

    "and" should {
      "return result by eq condition" in {
        val result = CurrencyDict.getUnsafe("alpha" is "RUB" and ("numeric" is "643"))
        result should be(Currency("RUB", 643, "Russian Ruble", "Russia").some)
      }

      "return result by eq and like conditions" in {
        val result = CurrencyDict.getUnsafe("alpha" is "RUB" and ("name" like "%Russian%"))
        result.map(_.numeric) should be(643.some)
      }

      "return empty result if one branch is false" in {
        val result = CurrencyDict.getUnsafe("alpha" is "RUB" and ("numeric" is "840"))
        result should be(None)
      }
    }

    "or" should {
      "return first result by eq condition" in {
        val result = CurrencyDict.getUnsafe("alpha" is "RUB" or ("alpha" is "MNT"))
        result.map(_.numeric) should be(643.some)
      }

      "return result by second branch" in {
        val result = CurrencyDict.getUnsafe("alpha" is "EUR" or ("alpha" is "MNT"))
        result.map(_.numeric) should be(496.some)
      }
    }
  }

  "a dictionary" when {
    "has numeric value field" should {
      "be loaded and return result" in {
        val result = ReversedRegionDict.getUnsafe("name" like "Saint%")
        result should be(78.some)
      }
    }

    "has double value field" should {
      "be loaded and return correct result" in {
        val result = DoubleValuesDict.getUnsafe("code" is "20").value
        assert(result === 105.55 +- 0.001)
      }
    }

    "has BigDecimal value field" should {
      "be loaded and return correct result" in {
        val result = BigDecimalValuesDict.getUnsafe("code" is "20").value
        assert(result === BigDecimal("100000000000000000000000000005.55") +- 0.001)
      }

      "return negative result" in {
        val result = BigDecimalValuesDict.getUnsafe("code" is "30").value
        assert(result === BigDecimal("-0.43") +- 0.001)
      }
    }
  }
}

private object RegionDict           extends MapIndexedDictionary[String]("region_dict")
private object ReversedRegionDict   extends MapIndexedDictionary[Int]("reversed_region_dict")
private object CurrencyDict         extends ProductIndexedDictionary[Currency, Currency]("currency_dict")
private object DoubleValuesDict     extends MapIndexedDictionary[Double]("double_values_dict")
private object BigDecimalValuesDict extends MapIndexedDictionary[BigDecimal]("bigdecimal_values_dict")
private object PopulationsDict      extends MapIndexedDictionary[Int]("populations_dict", inLowerCase = true)
