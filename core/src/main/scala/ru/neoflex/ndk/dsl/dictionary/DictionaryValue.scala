package ru.neoflex.ndk.dsl.dictionary

import ru.neoflex.ndk.dsl.DictLazyCondition
import ru.neoflex.ndk.error.NdkError

final case class DictionaryValue[T](var x: () => Either[NdkError, Option[T]], dictionaryName: String) {
  lazy val get: Either[NdkError, Option[T]] = {
    val t = x()
    x = null
    t
  }

  def ==(o: T): DictLazyCondition[T] = DictLazyCondition[T](this, _ == o)

  def map[V](f: T => V): DictionaryValue[V] = DictionaryValue[V](dictionaryName)(get.map(_.map(f)))
}

object DictionaryValue {
  def apply[T](dictionaryName: String)(x: => Either[NdkError, Option[T]]): DictionaryValue[T] =
    new DictionaryValue[T](() => x, dictionaryName)
}
