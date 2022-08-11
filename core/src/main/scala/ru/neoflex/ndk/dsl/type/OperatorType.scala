package ru.neoflex.ndk.dsl.`type`

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.dsl.{
  Action,
  Flow,
  FlowOp,
  ForEachOp,
  GatewayOp,
  PythonOperatorOp,
  RestService,
  RuleOp,
  TableOp,
  WhileOp
}

sealed trait OperatorType
object OperatorType {
  case object Action      extends OperatorType
  case object Rule        extends OperatorType
  case object Gateway     extends OperatorType
  case object Table       extends OperatorType
  case object ForEach     extends OperatorType
  case object While       extends OperatorType
  case object Flow        extends OperatorType
  case object PyOperator  extends OperatorType
  case object RestService extends OperatorType

  def apply(op: FlowOp): OperatorType = op match {
    case _: Action                 => Action
    case _: RuleOp                 => Rule
    case _: Flow                   => Flow
    case _: TableOp                => Table
    case _: GatewayOp              => Gateway
    case _: WhileOp                => While
    case _: ForEachOp              => ForEach
    case _: PythonOperatorOp[_, _] => PyOperator
    case _: RestService[_, _]      => RestService
  }

  def fromString(value: String): Option[OperatorType] = value match {
    case "Action"      => Action.some
    case "Rule"        => Rule.some
    case "Gateway"     => Gateway.some
    case "Table"       => Table.some
    case "ForEach"     => ForEach.some
    case "While"       => While.some
    case "Flow"        => Flow.some
    case "PyOperator"  => PyOperator.some
    case "RestService" => RestService.some
    case _             => None
  }
}
