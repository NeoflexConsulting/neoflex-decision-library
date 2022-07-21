package ru.neoflex.ndk.error

import pureconfig.error.ConfigReaderFailures
import ru.neoflex.ndk.dsl.dictionary.indexed.IndexType
import ru.neoflex.ndk.dsl.{ FlowOp, Table, TableOp }

sealed trait NdkError {
  def toThrowable: Throwable
}

final case class OperatorExecutionError(operator: FlowOp, error: Option[Throwable], details: Any*) extends NdkError {
  override def toThrowable: Throwable = {
    val message = s"Execution of the Operator[${operator.name}, ${operator.id}] has failed. Details: $details"
    error.map(new RuntimeException(message, _)).getOrElse(new RuntimeException(message))
  }
}
object OperatorExecutionError {
  def apply(operator: FlowOp, error: Throwable, details: Any*): OperatorExecutionError =
    OperatorExecutionError(operator, Some(error), details)
}
final case class WrappedError(error: Throwable) extends NdkError {
  override def toThrowable: Throwable = error
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

final case class PyOperatorStartError(op: FlowOp, error: Throwable) extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Error occurred while starting or getting python process of the operator[${op.name}, ${op.id}]",
      error
    )
}
final case class PyOperatorWritingError(op: FlowOp, error: Throwable) extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Error occurred while writing data to py process of the operator[${op.name}, ${op.id}]",
      error
    )
}
final case class PyOperatorExecutionError(op: FlowOp, exitCode: Int) extends NdkError {
  override def toThrowable: Throwable =
    new IllegalStateException(
      s"Error occurred while executing python operator[${op.name}, ${op.id}]. Exit code: $exitCode"
    )
}
final case class PyDataEncodeError(op: FlowOp, error: Throwable) extends NdkError {
  override def toThrowable: Throwable = new RuntimeException(
    s"Error occurred while encoding input data for python operator[${op.name}, ${op.id}]",
    error
  )
}
final case class PyDataDecodeError(data: String, op: FlowOp, error: Throwable) extends NdkError {
  override def toThrowable: Throwable = new RuntimeException(
    s"Error occurred while decoding result from python operator[${op.name}, ${op.id}]",
    error
  )
}

final case class RestServiceError(error: Throwable, op: FlowOp) extends NdkError {
  override def toThrowable: Throwable = error
}

final case class ConfigLoadError(failures: ConfigReaderFailures) extends NdkError {
  override def toThrowable: Throwable = new RuntimeException(failures.prettyPrint())
}

final case class DictionaryLoadingError(dictionaryName: String, message: String, error: Option[Throwable] = None)
    extends NdkError {
  override def toThrowable: Throwable = {
    val errorMessage = s"Dictionary[ $dictionaryName ] loading error: $message"
    error.map(new RuntimeException(errorMessage, _)).getOrElse(new RuntimeException(errorMessage))
  }
}

final case class FeelExpressionError(dictionaryName: String, expressionName: String, version: String, message: String)
    extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(
      s"Error has occurred with the FEEL expression. " +
        s"Dictionary: $dictionaryName, expression: $expressionName:$version, message: $message"
    )
}
final case class FieldNotIndexedError(dictionaryName: String, fieldName: String, indexType: IndexType)
    extends NdkError {
  override def toThrowable: Throwable =
    new RuntimeException(s"Field $fieldName is not indexed in dictionary $dictionaryName with index type: $indexType")
}
final case class NoSuchFieldInDictionaryRecord(fieldName: String) extends NdkError {
  override val toThrowable: Throwable = new RuntimeException(s"There is no such field in the dictionary: $fieldName")
}
final case class DictionaryFieldTypeMismatch(fieldName: Option[String], record: Any, error: Throwable) extends NdkError {
  override def toThrowable: Throwable = new RuntimeException("")
}

trait ErrorSyntax {
  type EitherError[A]     = Either[NdkError, A]
  type EitherThrowable[A] = Either[Throwable, A]
}
