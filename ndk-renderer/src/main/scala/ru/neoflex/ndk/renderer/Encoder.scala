package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl._

trait Encoder[A] extends (A => String)

trait Encoders {
  def action: Encoder[Action]
  def rule: Encoder[RuleOp]
  def flow: Encoder[Flow]
  def table: Encoder[TableOp]
  def gateway: Encoder[GatewayOp]
  def whileLoop: Encoder[WhileOp]
  def forEach: Encoder[ForEachOp]
  def generic: Encoder[FlowOp]
}

trait EncodersHolder {
  val encoders: Encoders
}

trait GenericEncoder extends Encoder[FlowOp] with EncodersHolder {
  override def apply(op: FlowOp): String =
    op match {
      case action: Action     => encoders.action(action)
      case rule: RuleOp       => encoders.rule(rule)
      case anotherFlow: Flow  => encoders.flow(anotherFlow)
      case table: TableOp     => encoders.table(table)
      case gateway: GatewayOp => encoders.gateway(gateway)
      case whileLoop: WhileOp => encoders.whileLoop(whileLoop)
      case forEach: ForEachOp => encoders.forEach(forEach)
    }
}
