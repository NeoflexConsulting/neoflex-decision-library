package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.{ catsSyntaxOptionId, toTraverseOps }
import ru.neoflex.ndk.dsl.dictionary.indexed.LikeIndex.LikeIndexRecord
import ru.neoflex.ndk.dsl.dictionary.indexed.RawIndexedDictionary.ValueExtractor
import ru.neoflex.ndk.error.NdkError

import scala.reflect.ClassTag

sealed trait DictIndex[V] {
  def find(k: Any): Seq[V]
}

class EqIndex[V](index: Map[String, V], lowerCased: Boolean) extends DictIndex[V] {
  override def find(k: Any): Seq[V] = {
    val key = if (lowerCased) k.toString.toLowerCase else k.toString
    index.get(key).toSeq
  }
}
object EqIndex {
  def apply[R: ValueExtractor, V: ClassTag](
    indexedField: String,
    valueField: Option[String],
    values: List[R],
    inLowerCase: Boolean
  ): Either[NdkError, EqIndex[IndexRecord[V]]] = {
    values.zipWithIndex.map {
      case (value, id) =>
        for {
          indexedFieldValue <- ValueExtractor[R].get[Any](value, indexedField.some)
          targetValue       <- ValueExtractor[R].get[V](value, valueField)
        } yield {
          val indexKey = if (inLowerCase) indexedFieldValue.toString.toLowerCase else indexedFieldValue.toString
          indexKey -> IndexRecord(id, targetValue)
        }
    }.sequence.map(i => new EqIndex(i.toMap, inLowerCase))
  }
}

class LikeIndex[V](values: List[LikeIndexRecord[V]], lowerCased: Boolean) extends DictIndex[V] {
  override def find(k: Any): Seq[V] = {
    val pattern = if (lowerCased) k.toString.toLowerCase else k.toString
    val searchFn: String => Boolean =
      if (pattern.startsWith("%") && pattern.endsWith("%") && pattern.length > 1) {
        val key = pattern.substring(1, pattern.length - 1)
        (s: String) => s.contains(key)
      } else if (pattern.startsWith("%")) {
        val key = pattern.substring(1)
        (s: String) => s.endsWith(key)
      } else if (pattern.endsWith("%")) {
        val key = pattern.substring(0, pattern.length - 1)
        (s: String) => s.startsWith(key)
      } else { (s: String) =>
        s == pattern
      }

    values.filter(r => searchFn(r.indexedValue)).map(_.record)
  }
}
object LikeIndex {
  def apply[R: ValueExtractor, V: ClassTag](
    indexedField: String,
    valueField: Option[String],
    values: List[R],
    inLowerCase: Boolean
  ): Either[NdkError, LikeIndex[IndexRecord[V]]] = {
    values.zipWithIndex.map {
      case (record, id) =>
        for {
          indexedFieldValue <- ValueExtractor[R].get[Any](record, indexedField.some)
          targetValue       <- ValueExtractor[R].get[V](record, valueField)
        } yield {
          val indexKey = if (inLowerCase) indexedFieldValue.toString.toLowerCase else indexedFieldValue.toString
          LikeIndexRecord(indexKey, IndexRecord[V](id, targetValue))
        }
    }.sequence.map(i => new LikeIndex[IndexRecord[V]](i, inLowerCase))
  }

  final case class LikeIndexRecord[V](indexedValue: String, record: V)
}

private[indexed] final case class IndexRecord[V](id: Int, value: V)
