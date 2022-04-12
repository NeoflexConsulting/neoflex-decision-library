package ru.neoflex.br.dsl

import ru.neoflex.br.dsl.Condition.CondExpr
import ru.neoflex.br.dsl.Gateway.When

sealed trait FlowOp

final case class Rule[T](name: String, body: Condition[T]) extends FlowOp
final case class Table(name: String)                       extends FlowOp

abstract class Flow(val name: String, val ops: Seq[FlowOp]) extends FlowOp
final case class SealedFlow(override val name: String, override val ops: Seq[FlowOp]) extends Flow(name, ops) {
  def apply(ops: FlowOp*): SealedFlow = copy(ops = ops)
}

final case class Gateway[T](name: String, whens: Seq[When[T]], otherwise: () => T) extends FlowOp

object Gateway {
  final case class SealedWhens[T](whens: Seq[When[T]]) {
    def and(wb: WhenBuilder[T]): WhenBuilder[T] = {
      wb.whens = whens
      wb
    }
    def otherwise(f: => T): Gateway[T] = Gateway("", whens, () => f)
  }
  final case class WhenBuilder[T](name: String, var whens: Seq[When[T]]) {
    private var exprValue: () => Boolean = _
    private var action: () => T          = _

    def apply(expr: => Boolean): WhenBuilder[T] = {
      exprValue = () => expr
      this
    }

    def run(f: => T): SealedWhens[T] = {
      action = () => f
      SealedWhens(whens :+ toWhen)
    }

    def toWhen: When[T] = When(name, exprValue, action)
  }

  final case class When[T](name: String, cond: () => Boolean, run: () => T)
}

final case class Condition[T](
  expr: () => Boolean,
  leftBranch: CondExpr[T] = () => None,
  rightBranch: CondExpr[T] = () => None) {

  def andThen(left: => T): Condition[T] = copy(leftBranch = () => Option(left))

  def otherwise(right: => T): Condition[T] = copy(rightBranch = () => Option(right))
}

object Condition {
  type CondExpr[T] = () => Option[T]
}
