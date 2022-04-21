package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.Table.{ ActionDef, Expression }
import syntax.NoId

trait Constants {
  val NoId   = "NoId"
  val NoName = "NoName"
}

sealed trait FlowOp {
  def id: String           = NoId
  def name: Option[String] = None
}

abstract class Action(override val id: String, val f: () => Unit, override val name: Option[String] = None)
    extends (() => Unit)
    with FlowOp {
  def this(f: => Unit) = {
    this(NoId, () => f)
  }

  def this(id: String, f: => Unit) = {
    this(id, () => f)
  }

  override def apply(): Unit = f()
}
final case class SealedAction(
  override val f: () => Unit,
  override val id: String = NoId,
  override val name: Option[String] = None)
    extends Action(id, f, name)

final case class Rule(override val id: String, override val name: Option[String], body: Condition) extends FlowOp

abstract class Flow(override val id: String, val ops: Seq[FlowOp], override val name: Option[String] = None)
    extends FlowOp {
  def this(id: String, op: FlowOp) = {
    this(id, Seq(op))
  }

  def this(id: String, name: Option[String], op: FlowOp) = {
    this(id, Seq(op), name)
  }

  def this(id: String, name: Option[String], ops: Seq[FlowOp]) = {
    this(id, ops, name)
  }
}
final case class SealedFlow(override val id: String, override val name: Option[String], override val ops: Seq[FlowOp])
    extends Flow(id, name, ops) {
  def apply(ops: FlowOp*): SealedFlow = copy(ops = ops)
}

trait TableOp extends FlowOp {
  val expressions: List[Expression]
  val actions: List[ActionDef]
  val conditions: List[Table.Condition]
  val actionsByName: Map[String, ActionDef]
}

trait GatewayOp extends FlowOp {
  val whens: Seq[When]
  val otherwise: FlowOp
}

trait WhileOp extends FlowOp {
  def condition: () => Boolean
  def body: FlowOp
}

trait ForEachOp extends FlowOp {
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
  def rule(id: String = NoId, name: Option[String] = None)(body: => Condition): Rule = {
    Rule(id, name, body)
  }

  def condition(expr: => Boolean): Condition = {
    Condition(() => expr)
  }
}

trait FlowSyntax {
  def flow: SealedFlow                                          = SealedFlow(NoId, None, Seq.empty)
  def flow(id: String, name: Option[String] = None): SealedFlow = SealedFlow(id, name, Seq.empty)
  def flowOps(ops: FlowOp*): Seq[FlowOp]                        = ops
}

trait ActionSyntax {
  def action(f: => Unit): SealedAction                                                 = SealedAction(() => f)
  def action(id: String = NoId, name: Option[String] = None)(f: => Unit): SealedAction = SealedAction(() => f, id, name)
}
