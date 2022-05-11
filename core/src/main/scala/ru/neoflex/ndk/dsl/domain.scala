package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.Rule.{ Condition, Otherwise }
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

trait Action extends FlowOp with (() => Unit) {
  val f: () => Unit
}

trait RuleOp extends FlowOp {
  def conditions: Seq[Condition]
  def otherwise: Option[Otherwise]
}

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
  def elementClass: Option[Class[_]]
}

trait FlowSyntax {
  def flow: SealedFlow                                          = SealedFlow(NoId, None, Seq.empty)
  def flow(id: String, name: Option[String] = None): SealedFlow = SealedFlow(id, name, Seq.empty)
  def flowOps(ops: FlowOp*): Seq[FlowOp]                        = ops
}
