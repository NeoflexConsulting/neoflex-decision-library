package ru.neoflex.ndk.dsl

import cats.syntax.option._
import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.Table.{ ActionDef, Expression }

sealed trait FlowOp

final case class Action(f: () => Unit, name: Option[String] = None) extends (() => Unit) with FlowOp {
  override def apply(): Unit = f()
}
final case class Rule(name: String, body: Condition) extends FlowOp

abstract class Flow(val name: String, val ops: Seq[FlowOp]) extends FlowOp
final case class SealedFlow(override val name: String, override val ops: Seq[FlowOp]) extends Flow(name, ops) {
  def apply(ops: FlowOp*): SealedFlow = copy(ops = ops)
}

trait TableOp extends FlowOp {
  val name: String
  val expressions: List[Expression]
  val actions: List[ActionDef]
  val conditions: List[Table.Condition]
}

trait GatewayOp extends FlowOp {
  val name: String
  val whens: Seq[When]
  val otherwise: FlowOp
}

trait WhileOp extends FlowOp {
  def name: String
  def condition: () => Boolean
  def body: FlowOp
}

trait ForEachOp extends FlowOp {
  def name: String
  def collection: () => Iterable[Any]
  def body: Any => FlowOp
}

final case class Condition(expr: () => Boolean, leftBranch: () => Unit = () => (), rightBranch: () => Unit = () => ()) {}

trait ConditionImplicits {
  implicit class ConditionOps(c: Condition) {
    def andThen(left: => Unit): Condition    = c.copy(leftBranch = () => left)
    def otherwise(right: => Unit): Condition = c.copy(rightBranch = () => right)
  }
}

trait RuleSyntax {
  def rule(name: String)(body: => Condition): Rule = {
    Rule(name, body)
  }

  def condition(expr: => Boolean): Condition = {
    Condition(() => expr)
  }
}

trait FlowSyntax {
  def flow(name: String): SealedFlow     = SealedFlow(name, Seq.empty)
  def flow(ops: FlowOp*): SealedFlow     = SealedFlow("", ops)
  def flowOps(ops: FlowOp*): Seq[FlowOp] = ops
}

trait ActionSyntax {
  def action(f: => Unit): Action               = Action(() => f)
  def action(name: String)(f: => Unit): Action = Action(() => f, name.some)
}
