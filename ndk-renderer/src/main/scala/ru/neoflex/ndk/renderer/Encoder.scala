package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl._

trait Encoder[A <: FlowOp] extends (EncodingContext[A] => String)

trait Encoders {
  def action: Encoder[Action]
  def rule: Encoder[RuleOp]
  def flow: Encoder[Flow]
  def table: Encoder[TableOp]
  def gateway: Encoder[GatewayOp]
  def whileLoop: Encoder[WhileOp]
  def forEach: Encoder[ForEachOp]
  def generic: Encoder[FlowOp]
  def nameOnly: Encoder[FlowOp]
}

trait EncodersHolder {
  val encoders: Encoders
}

trait GenericEncoder extends Encoder[FlowOp] with EncodersHolder {
  override def apply(ctx: EncodingContext[FlowOp]): String =
    ctx.op match {
      case _: Action    => encoders.action(ctx.cast)
      case _: RuleOp    => encoders.rule(ctx.cast)
      case _: Flow      => encoders.flow(ctx.cast)
      case _: TableOp   => encoders.table(ctx.cast)
      case _: GatewayOp => encoders.gateway(ctx.cast)
      case _: WhileOp   => encoders.whileLoop(ctx.cast)
      case _: ForEachOp => encoders.forEach(ctx.cast)
    }
}

trait DepthLimitedEncoder extends GenericEncoder {
  override def apply(ctx: EncodingContext[FlowOp]): String = {
    if (!ctx.op.isEmbedded && ctx.depthLimitReached) {
      encoders.nameOnly(ctx)
    } else {
      super.apply(ctx.incCurrentDepth())
    }
  }
}

final case class EncodingContext[A <: FlowOp](
  op: A,
  userDefinedOperatorEncodingDepth: Int,
  currentEncodingDepth: Int,
  limitEncodingDepth: Boolean = true) {

  def cast[B <: A]: EncodingContext[B] = this.asInstanceOf[EncodingContext[B]]

  def incCurrentDepth(): EncodingContext[A] = copy(currentEncodingDepth = currentEncodingDepth + 1)

  def depthLimitReached: Boolean = limitEncodingDepth && userDefinedOperatorEncodingDepth <= currentEncodingDepth

  def withOperator[B <: FlowOp](x: B): EncodingContext[B] = copy(op = x)

  def withOperatorAndDepth[B <: FlowOp](x: B): EncodingContext[B] =
    copy(op = x, currentEncodingDepth = currentEncodingDepth + 1)
}
