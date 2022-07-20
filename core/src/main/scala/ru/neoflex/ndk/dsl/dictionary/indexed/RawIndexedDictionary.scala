package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.toTraverseOps
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Json }
import ru.neoflex.ndk.error.{ NdkError, NoSuchFieldInDictionaryRecord }

final case class RawIndexedDictionary[V](table: List[V], indexedFields: Set[IndexedField], valueField: Option[String])

object RawIndexedDictionary {
  implicit val indexedFieldDecoder: Decoder[IndexedField]                = deriveDecoder[IndexedField]
  implicit def dictDecoder[V: Decoder]: Decoder[RawIndexedDictionary[V]] = deriveDecoder[RawIndexedDictionary[V]]

  sealed trait ValueExtractor[T] {
    def get[V](record: T, name: Option[String]): Either[NdkError, V]
  }
  object ValueExtractor {
    def apply[T: ValueExtractor]: ValueExtractor[T] = implicitly[ValueExtractor[T]]
  }
  implicit val mapDictValueExtractor: ValueExtractor[Map[String, Json]] = new ValueExtractor[Map[String, Json]] {
    override def get[V](record: Map[String, Json], name: Option[String]): Either[NdkError, V] = {
      name
        .flatMap(record.get)
        .map(_.foldJson().asInstanceOf[V])
        .toRight(NoSuchFieldInDictionaryRecord(name.getOrElse("")))
    }
  }
  implicit def productDictValueExtractor[T <: Product]: ValueExtractor[T] = new ValueExtractor[T] {
    override def get[V](record: T, name: Option[String]): Either[NdkError, V] = {
      name match {
        case Some(n) =>
          record.productElementNames.zipWithIndex
            .find(_._1 == n)
            .map(_._2)
            .toRight(NoSuchFieldInDictionaryRecord(name.getOrElse("")))
            .map { idx =>
              record.productElement(idx).asInstanceOf[V]
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
