package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.syntax.either._
import cats.implicits.{ catsSyntaxOptionId, toTraverseOps }
import io.circe.{ Decoder, Json }
import ru.neoflex.ndk.dsl.dictionary.indexed.RawIndexedDictionary.ValueExtractor
import ru.neoflex.ndk.dsl.dictionary.{ DictionaryLoader, DictionaryValue }
import ru.neoflex.ndk.error.{ DictionaryLoadingError, FieldNotIndexedError, NdkError }

import scala.reflect.{ classTag, ClassTag }

sealed abstract class IndexedDictionary[R: ValueExtractor: Decoder, V: ClassTag](
  dictionaryName: String,
  inLowerCase: Boolean = false,
  eagerLoad: Boolean = true) {

  private lazy val _indexes = buildDictionaryIndexes()

  if (eagerLoad) {
    _indexes.fold(e => throw e.toThrowable, x => x)
  }

  private def findValue(c: LeafCondition[_], indexType: IndexType) = {
    import c._
    for {
      indexes <- _indexes
      idx <- indexes
              .get(IndexKey(fieldName, indexType))
              .toRight(FieldNotIndexedError(dictionaryName, fieldName, indexType))
    } yield idx.find(value)
  }

  private def findValue(condition: SearchConditionOperator): Either[NdkError, Seq[IndexRecord[V]]] =
    condition match {
      case And(left, right) =>
        for {
          leftValue     <- findValue(left)
          rightValueIds <- findValue(right).map(_.map(_.id).toSet)
        } yield leftValue.filter(r => rightValueIds.contains(r.id))
      case Or(left, right) =>
        for {
          leftValue  <- findValue(left)
          rightValue <- findValue(right)
        } yield leftValue ++ rightValue
      case c: Eq[_]   => findValue(c, EqIndexType)
      case c: Like[_] => findValue(c, LikeIndexType)
    }

  def apply(condition: SearchConditionOperator): DictionaryValue[V] = {
    DictionaryValue(dictionaryName) {
      findValue(condition).map(_.headOption.map(_.value))
    }
  }

  def getUnsafe(condition: SearchConditionOperator): Option[V] =
    apply(condition).get.fold(e => throw e.toThrowable, x => x)

  private def buildDictionaryIndexes(): Either[NdkError, Map[IndexKey, DictIndex[IndexRecord[V]]]] = {
    for {
      dictionary <- DictionaryLoader.loadDictionary[RawIndexedDictionary[R]](dictionaryName)
      valueField <- getValueFieldName(dictionary)
      indexes    <- buildIndexes(dictionary, valueField)
    } yield indexes
  }

  private def buildIndexes(
    dictionary: RawIndexedDictionary[R],
    valueField: Option[String]
  ): Either[NdkError, Map[IndexKey, DictIndex[IndexRecord[V]]]] = {
    dictionary.indexedFields.map { f =>
      f.indexTypes.map {
        _.map {
          case EqIndexType =>
            EqIndex[R, V](f.name, valueField, dictionary.table, inLowerCase).map { index =>
              IndexKey(f.name, EqIndexType) -> index
            }
          case LikeIndexType =>
            LikeIndex[R, V](f.name, valueField, dictionary.table, inLowerCase).map { index =>
              IndexKey(f.name, LikeIndexType) -> index
            }
        }
      }.leftMap(e => DictionaryLoadingError(dictionaryName, "Index types parse error", e.some))
    }.toList.sequence
      .flatMap(_.flatten.sequence)
      .map(_.toMap)
  }

  protected def getValueFieldName(dictionary: RawIndexedDictionary[R]): Either[NdkError, Option[String]] = {
    dictionary.valueField.map(f => Right(f.some)).getOrElse {
      for {
        firstRecord <- dictionary.table.headOption.toRight(
                        DictionaryLoadingError(
                          dictionaryName,
                          "Neither valueField isn't defined nor any dictionary record exists",
                          None
                        )
                      )
        fieldNames = recordFieldNames(firstRecord)
        _ <- Either.cond(
              fieldNames.size == 2,
              fieldNames,
              DictionaryLoadingError(
                dictionaryName,
                "Without valueField definition, each dictionary record should have only two fields",
                None
              )
            )
        valueField <- fieldNames
                       .filterNot(dictionary.indexedFields.map(_.name))
                       .headOption
                       .toRight(
                         DictionaryLoadingError(
                           dictionaryName,
                           "Without valueField definition, one field should be indexed, " +
                             "but another should be value field",
                           None
                         )
                       )
      } yield valueField.some
    }
  }

  protected def recordFieldNames(record: R): Set[String]
}

abstract class MapIndexedDictionary[V: Decoder: ClassTag](
  dictionaryName: String,
  inLowerCase: Boolean = false,
  eagerLoad: Boolean = true)
    extends IndexedDictionary[Map[String, Json], V](dictionaryName, inLowerCase, eagerLoad) {

  override protected def recordFieldNames(record: Map[String, Json]): Set[String] = record.keySet
}

abstract class ProductIndexedDictionary[R <: Product: Decoder: ClassTag, V: ClassTag](
  dictionaryName: String,
  inLowerCase: Boolean = false,
  eagerLoad: Boolean = true)
    extends IndexedDictionary[R, V](dictionaryName, inLowerCase, eagerLoad) {

  override protected def recordFieldNames(record: R): Set[String] = record.productElementNames.toSet

  override protected def getValueFieldName(dictionary: RawIndexedDictionary[R]): Either[NdkError, Option[String]] = {
    if (classTag[R] == classTag[V]) {
      Right(None)
    } else {
      super.getValueFieldName(dictionary)
    }
  }
}

private[indexed] final case class IndexKey(fieldName: String, indexType: IndexType)
