package ru.neoflex.ndk.error

import ru.neoflex.ndk.dsl.{ FlowOp, Table, TableOp }

sealed trait NdkError {
  def toThrowable: Throwable
}

final case class OperatorExecutionError(operator: FlowOp, error: Throwable, details: Any*) extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Execution of the Operator[${operator.name}, ${operator.id}] has failed. Details: $details",
      error
    )
}
final case class TableActionNotFound(table: TableOp, actionName: String) extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Action[$actionName] not found in the Table[${table.name}, ${table.id}]"
    )
}
final case class ActionArgumentsMatchError(table: TableOp, action: Table.ActionDef, args: Table.Args) extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Arguments for the action[${action.name}](number of args: ${action.argsCount}) doesn't matches in " +
        s"the Table[${table.name}, ${table.id}]. Number of the specified args: ${args.count}"
    )
}
final case class ExpressionsAndConditionsNumberMatchError(table: TableOp, failedCondition: Table.Condition)
    extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Number of the expressions and conditions doesn't matches in the Table[${table.name}, ${table.id}]. " +
        s"Failed condition: $failedCondition"
    )
}
final case class OperatorsTypeMatchError(first: FlowOp, second: FlowOp, message: String = "") extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Operators type is different, first[${first.id}, ${first.getClass}], second[${second.id}, ${second.getClass}]. $message"
    )
}

trait ErrorSyntax {
  type EitherError[A] = Either[NdkError, A]
}
