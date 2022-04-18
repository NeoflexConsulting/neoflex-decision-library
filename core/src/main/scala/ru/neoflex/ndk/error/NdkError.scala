package ru.neoflex.ndk.error

import ru.neoflex.ndk.dsl.{ FlowOp, Table, TableOp }

trait NdkError
final case class OperatorExecutionError(operator: FlowOp, error: Throwable, details: Any*)            extends NdkError
final case class TableActionNotFound(table: TableOp, actionName: String)                              extends NdkError
final case class ActionArgumentsMatchError(table: TableOp, action: Table.ActionDef, args: Table.Args) extends NdkError
final case class ExpressionsAndConditionsNumberMatchError(table: TableOp, failedCondition: Table.Condition)
    extends NdkError

trait ErrorSyntax {
  type EitherError[A] = Either[NdkError, A]
}
