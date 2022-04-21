package ru.neoflex.ndk.testkit

import org.scalatest.Assertions
import ru.neoflex.ndk.dsl.syntax.EitherError
import org.scalactic.source

trait FlowMatchers {
  def fired(): Matcher                = FiredOperatorMarcher(true)
  def notFired(): Matcher             = FiredOperatorMarcher(false)
  def executedRows(num: Int): Matcher = ExecutedRows(num)
  def oneExecutedRow(): Matcher       = ExecutedRows(1)
}

sealed trait Matcher {
  def assertMatch(
    id: String,
    operatorType: OperatorType,
    tracker: FlowExecutionTracker[EitherError]
  )(implicit pos: source.Position
  ): Unit
}

final case class FiredOperatorMarcher(shouldBeFired: Boolean) extends Matcher {
  def assertMatch(
    id: String,
    operatorType: OperatorType,
    tracker: FlowExecutionTracker[EitherError]
  )(implicit pos: source.Position
  ): Unit = {
    val operatorExecStatus = tracker.status(operatorType, id)
    val operatorFireState = if (shouldBeFired) {
      operatorExecStatus.contains(Finished)
    } else {
      operatorExecStatus.isEmpty
    }
    Assertions.assert(operatorFireState, s"Operator $id was ${if (shouldBeFired) "not " else ""}executed")
  }
}

final case class ExecutedRows(num: Int) extends Matcher {
  def assertMatch(
    id: String,
    operatorType: OperatorType,
    tracker: FlowExecutionTracker[EitherError]
  )(implicit pos: source.Position
  ): Unit = {
    def transformDetails[T](pf: PartialFunction[ExecutionDetails, T]) = tracker.details(operatorType, id).map { d =>
      if (pf.isDefinedAt(d)) pf(d)
      else {
        Assertions.fail(s"Execution details is not of type ${classOf[TableExecutionDetails]}: $d")
      }
    }

    val numOfExecutedRows = transformDetails {
      case TableExecutionDetails(_, executedRows) => executedRows
    }
    val specifiedRowsWasExecuted = transformDetails {
      case TableExecutionDetails(status, executedRows) => status == Finished && num == executedRows
    }.exists(x => x)

    Assertions.assert(
      specifiedRowsWasExecuted,
      s"The specified number of required rows does not match with executed from the table $id. " +
        s"Number of executed rows: $numOfExecutedRows"
    )
  }
}
