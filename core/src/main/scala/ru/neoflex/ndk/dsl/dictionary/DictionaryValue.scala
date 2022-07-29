package ru.neoflex.ndk.dsl.dictionary

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.dsl.DictLazyCondition
import ru.neoflex.ndk.error.NdkError

import scala.math.Ordering.Implicits.infixOrderingOps

final case class DictionaryValue[T](var x: () => Either[NdkError, Option[T]], dictionaryName: String, key: Option[String] = None) {
  lazy val get: Either[NdkError, Option[T]] = {
    val t = x()
    x = null
    t
  }

  def ==(o: T): DictLazyCondition[T] = DictLazyCondition[T](this, _ == o)

  def >=(o: T)(implicit ev: Ordering[T]): DictLazyCondition[T] = DictLazyCondition[T](this, _ >= o)
  def <=(o: T)(implicit ev: Ordering[T]): DictLazyCondition[T] = DictLazyCondition[T](this, _ <= o)
  def >(o: T)(implicit ev: Ordering[T]): DictLazyCondition[T]  = DictLazyCondition[T](this, _ > o)
  def <(o: T)(implicit ev: Ordering[T]): DictLazyCondition[T]  = DictLazyCondition[T](this, _ < o)

  def map[V](f: T => V): DictionaryValue[V] = DictionaryValue[V](dictionaryName)(get.map(_.map(f)))
}

object DictionaryValue {
  val DictFileExtension = "yaml"

  def apply[T](dictionaryName: String)(x: => Either[NdkError, Option[T]]): DictionaryValue[T] =
    new DictionaryValue[T](() => x, dictionaryName)

  def apply[T](dictionaryName: String, key: String)(x: => Either[NdkError, Option[T]]): DictionaryValue[T] =
    new DictionaryValue[T](() => x, dictionaryName, key.some)
}
