package ru.neoflex.ndk.dsl.dictionary.indexed

sealed trait SearchConditionOperator

final case class And(left: SearchConditionOperator, right: SearchConditionOperator) extends SearchConditionOperator
final case class Or(left: SearchConditionOperator, right: SearchConditionOperator) extends SearchConditionOperator

sealed trait LeafCondition[T] extends SearchConditionOperator {
  def fieldName: String
  def value: T
}

final case class Eq[T](fieldName: String, override val value: T) extends LeafCondition[T]
final case class Like[T](fieldName: String, override val value: T) extends LeafCondition[T]

trait SearchConditionOperatorImplicits {
  implicit class StringSearchOps(s: String) {
    def like[T](v: T): SearchConditionOperator = Like(s, v)
    def is[T](v: T): SearchConditionOperator = Eq(s, v)
  }

  implicit class SearchOperatorOps(l: SearchConditionOperator) {
    def and(r: SearchConditionOperator): SearchConditionOperator = And(l, r)
    def or(r: SearchConditionOperator): SearchConditionOperator = Or(l, r)
  }
}
