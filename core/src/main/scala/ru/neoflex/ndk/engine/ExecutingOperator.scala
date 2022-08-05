package ru.neoflex.ndk.engine

import cats.syntax.option._
import ru.neoflex.ndk.dsl.FlowOp

final case class ExecutingOperator[T <: FlowOp](op: T, root: FlowOp, parent: Option[FlowOp] = None) {
  def id: String           = op.id
  def name: Option[String] = op.name
}

object ExecutingOperator {
  implicit class Ops[T <: FlowOp](op: T) {
    def withParent(root: FlowOp, parent: FlowOp): ExecutingOperator[T] = ExecutingOperator(op, root, parent.some)
    def withParentFrom(o: ExecutingOperator[T]): ExecutingOperator[T]  = ExecutingOperator(op, o.root, o.parent)
  }
}
