package ru.neoflex.ndk.dsl.dictionary

import io.circe.{ Decoder, Json }

import java.io.InputStreamReader
import scala.util.Using
import io.circe.yaml.parser.parse
import ru.neoflex.ndk.error.{ DictionaryLoadingError, NdkError }
import cats.syntax.either._
import cats.syntax.option._

object DictionaryLoader {
  def loadDictionary[T: Decoder](dictionaryName: String): Either[NdkError, T] = {
    Using(new InputStreamReader(getClass.getResourceAsStream(s"/$dictionaryName.${DictionaryValue.DictFileExtension}")))(parse).toEither.joinRight
      .flatMap(implicitly[Decoder[T]].decodeJson)
      .leftMap { t =>
        DictionaryLoadingError(dictionaryName, t.getMessage, t.some)
      }
  }

  def loadDictionary[T: Decoder](dictionaryName: String, rootField: String): Either[NdkError, T] = {
    loadDictionary[Map[String, Json]](dictionaryName).flatMap { map =>
      map.get(rootField).toRight(DictionaryLoadingError(dictionaryName, s"There is no field $rootField", None))
    }.flatMap { j =>
      implicitly[Decoder[T]].decodeJson(j).leftMap { f =>
        DictionaryLoadingError(dictionaryName, "Json decoding error", f.some)
      }
    }
  }
}
