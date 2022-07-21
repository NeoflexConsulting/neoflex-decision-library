package ru.neoflex.ndk.dsl

import cats.syntax.either._
import ru.neoflex.ndk.dsl.dictionary.DictionaryValue
import ru.neoflex.ndk.error.{ NdkError, WrappedError }

import scala.util.control.Exception.nonFatalCatch

sealed trait LazyCondition {
  def eval(): Either[NdkError, Boolean]
}

final case class SimpleLazyCondition(c: () => Boolean) extends LazyCondition {
  override def eval(): Either[NdkError, Boolean] = nonFatalCatch.either(c()).leftMap(WrappedError)
}

trait LazyConditionImplicits {
  implicit def toLazyCondition(c: => Boolean): LazyCondition = SimpleLazyCondition(() => c)
}

final case class DictLazyCondition[T](v: DictionaryValue[T], cond: T => Boolean) extends LazyCondition {
  def eval(): Either[NdkError, Boolean] = v.get.map(_.exists(cond))
}
