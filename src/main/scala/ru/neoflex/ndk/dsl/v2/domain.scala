package ru.neoflex.ndk.dsl.v2


trait Table
/*

final case class Table1[A](f: () => A, conditionsList: List[Condition1[A]] = List.empty) extends Table {
//  def conditions(conditions: Condition1[A]*): Table1[A] = copy(conditionsList = conditions.toList)
  def conditions(c: Condition1[A]): Table1[A] = copy(conditionsList = conditions.toList)
}
final case class Table2[A, B](f1: () => A, f2: () => B) extends Table
final case class Table3[A, B, C](f1: () => A, f2: () => B, f3: () => C, conditionsList: List[Condition3[A, B, C]] = List.empty) extends Table {
  def conditions(conditions: Condition3[A, B, C]*): Table3[A, B, C] = copy(conditionsList = conditions.toList)
}

trait TableSyntax {
  def table(name: String)(build: => Table): Table = build

  def expressions[A](f: => A): Table1[A] = Table1(() => f)
  def expressions[A, B](f1: => A, f2: => B): Table2[A, B] = Table2(() => f1, () => f2)
  def expressions[A, B, C](f1: => A, f2: => B, f3: => C): Table3[A, B, C] = Table3(() => f1, () => f2, () => f3)

  def c[A](f: A => Boolean)(action: => Unit): Condition1[A] = Condition1(f, () => action)
  def c[A, B, C](f1: A => Boolean, f2: B => Boolean, f3: C => Boolean)(action: => Unit): Condition3[A, B, C] = Condition1(f, () => action)
}

trait TableImplicits {
  implicit def function1ToCondition[A](f: A => Boolean): Condition1[A] = Condition1(f, () => ())
//  implicit def function3ToCondition[A, B, C](f: (A, B, C) => Boolean): Condition3[A, B, C] = Condition3(f, () => ())
/*
  implicit class Condition1Ops[A](f: A => Boolean) {
    def withAction(action: () => Unit): Condition1[A] = Condition1(f, action)
  }*/
}

object Table {
  type CondExpr1[A] = A => Boolean
  type CondExpr2[A, B] = (A, B) => Boolean
  type CondExpr3[A, B, C] = (A, B, C) => Boolean

  final case class Condition1[A](expr: CondExpr1[A], action: () => Unit)
  final case class Condition3[A, B, C](expr: CondExpr3[A, B, C], action: () => Unit)
}

object Test extends TableSyntax with TableImplicits {

  table("Table 1") {
    expressions (1) conditions(
      c[Int](_ == 1) {

      }
    )
  }

  table("Simple Table") {
    expressions (
      1,
      "",
      "Hello"
    ) conditions (
      c(_ == 1, _.isEmpty, _ == "Hello")
    )
  }
}
*/
