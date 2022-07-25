package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.{ catsSyntaxOptionId, toTraverseOps }
import cats.syntax.either._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Json }
import ru.neoflex.ndk.error.{ DictionaryFieldTypeMismatch, NdkError, NoSuchFieldInDictionaryRecord }

import scala.reflect.{ classTag, ClassTag }
import scala.util.control.Exception.nonFatalCatch

final case class RawIndexedDictionary[V](table: List[V], indexedFields: Set[IndexedField], valueField: Option[String])

object RawIndexedDictionary {
  implicit val indexedFieldDecoder: Decoder[IndexedField]                = deriveDecoder[IndexedField]
  implicit def dictDecoder[V: Decoder]: Decoder[RawIndexedDictionary[V]] = deriveDecoder[RawIndexedDictionary[V]]

  sealed trait ValueExtractor[T] {
    def get[V: ClassTag](record: T, name: Option[String]): Either[NdkError, V]
  }

  object ValueExtractor {
    def apply[T: ValueExtractor]: ValueExtractor[T] = implicitly[ValueExtractor[T]]
  }

  implicit val mapDictValueExtractor: ValueExtractor[Map[String, Json]] = new ValueExtractor[Map[String, Json]] {
    override def get[V: ClassTag](record: Map[String, Json], name: Option[String]): Either[NdkError, V] = {
      name
        .flatMap(record.get)
        .toRight(NoSuchFieldInDictionaryRecord(name.getOrElse("")))
        .flatMap { j =>
          nonFatalCatch.either {
            val foldedJsonValue = j.foldJson()
            JsonNumberConverter
              .convert[V](foldedJsonValue) { v =>
                classTag[V].runtimeClass.cast(v)
              }
              .map(_.asInstanceOf[V])
          }.leftMap { e =>
            DictionaryFieldTypeMismatch(name, record, e.some)
          }.flatMap {
            _.toRight(DictionaryFieldTypeMismatch(name, record))
          }
        }
    }
  }

  implicit def productDictValueExtractor[T <: Product]: ValueExtractor[T] = new ValueExtractor[T] {
    override def get[V: ClassTag](record: T, name: Option[String]): Either[NdkError, V] = {
      name match {
        case Some(n) =>
          record.productElementNames.zipWithIndex
            .find(_._1 == n)
            .map(_._2)
            .toRight(NoSuchFieldInDictionaryRecord(name.getOrElse("")))
            .flatMap { idx =>
              nonFatalCatch.either {
                val value = record.productElement(idx)
                JavaToScalaPrimitivesConverter
                  .convert(value) { v =>
                    classTag[V].runtimeClass.cast(v)
                  }
                  .asInstanceOf[V]
              }.leftMap { e =>
                DictionaryFieldTypeMismatch(name, record, e.some)
              }
            }
        case None => Right(record.asInstanceOf[V])
      }
    }
  }

  implicit class JsonOps(json: Json) {
    def foldJson(): Any = foldJson(json)

    def foldJson(j: Json): Any = {
      j.fold(
        null,
        b => b,
        n => n,
        s => s,
        _.map(foldJson),
        _.toMap.map {
          case (k, v) =>
            (k, foldJson(v))
        }
      )
    }
  }
}

final case class IndexedField(`type`: String, name: String) {
  def indexTypes: Either[Throwable, Set[IndexType]] =
    `type`
      .split("\\|")
      .map { (t: String) =>
        t match {
          case "eq"   => Right(EqIndexType)
          case "like" => Right(LikeIndexType)
          case _      => Left(new MatchError(t))
        }
      }
      .toList
      .sequence
      .map(_.toSet)
}
