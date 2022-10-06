package ru.neoflex.ndk.engine.observer

sealed trait ExecutionDetails

final case class ExecutedBranch(name: String, dictConditionDetails: Option[DictConditionDetails])
    extends ExecutionDetails
final case class DictConditionDetails(dict: String, key: Option[String], condition: String)

final case class TableExpressionValue(name: String, valueDetails: ExpressionValueDetails)

sealed trait ExpressionValueDetails
final case class ExpressionSimpleValue(value: String)                                  extends ExpressionValueDetails
final case class ExpressionDictValue(dict: String, key: Option[String], value: String) extends ExpressionValueDetails

final case class TableExecutionResult(expressionValues: Seq[TableExpressionValue], executedRows: Seq[String])
    extends ExecutionDetails
