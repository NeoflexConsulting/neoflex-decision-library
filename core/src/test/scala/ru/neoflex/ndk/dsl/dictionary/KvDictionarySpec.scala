package ru.neoflex.ndk.dsl.dictionary

import org.scalatest.{EitherValues, Inside}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.neoflex.ndk.error.DictionaryLoadingError

class KvDictionarySpec extends AnyWordSpec with EitherValues with Matchers with Inside {
  "A dict" when {
    "has a value" should {
      "return value" in {
        val result = SimpleDictionary("v1").get.value
        result should be (Some(1))
      }
    }

    "doesn't have a value" should {
      "return empty result" in {
        val result = SimpleDictionary("undefined").get.value
        result should be (None)
      }
    }

    "haven't been loaded" should {
      "throw exception" in {
        a[RuntimeException] should be thrownBy new KvDictionary[Int]("nonexistent_dict") {}
      }

      "swallow exception before next operation" in {
        val dict = new KvDictionary[Int]("nonexistent_dict", eagerLoad = false) {}
        val result = dict("some_key").get
        inside(result.left.value) {
          case DictionaryLoadingError(_, _, _) =>
        }
      }
    }

    "type is mismatched" should {
      "results in error" in {
        val dict = new KvDictionary[String]("kv_dict", eagerLoad = false) {}
        val result = dict("v1").get
        inside(result.left.value) {
          case DictionaryLoadingError(_, _, _) =>
        }
      }
    }
  }
}

object SimpleDictionary extends KvDictionary[Int]("kv_dict")
