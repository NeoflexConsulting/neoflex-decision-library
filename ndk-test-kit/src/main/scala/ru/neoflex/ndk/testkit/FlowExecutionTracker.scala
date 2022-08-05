package ru.neoflex.ndk.testkit

import cats.MonadError
import cats.syntax.applicative._
import ru.neoflex.ndk.dsl.syntax.NoId
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.`type`.OperatorType
import ru.neoflex.ndk.engine.{ ExecutingOperator, FlowExecutionObserver }
import ru.neoflex.ndk.error.NdkError

class FlowExecutionTracker[F[_]](implicit monadError: MonadError[F, NdkError]) extends FlowExecutionObserver[F] {
  private val executionDetails = collection.mutable.Map[(OperatorType, String), ExecutionDetails]()

  private def started[O <: FlowOp](execOperator: ExecutingOperator[O]): F[O] = {
    ifHasId(execOperator.id) {
      executionDetails((OperatorType(execOperator.op), execOperator.id)) = Started
    }
    execOperator.op.pure
  }

  private def finished[O <: FlowOp](execOp: ExecutingOperator[O]): F[Unit] = {
    ifHasId(execOp.id) {
      executionDetails((OperatorType(execOp.op), execOp.id)) = Finished
    }
    ().pure
  }

  private def ifHasId(id: String)(body: => Unit): Unit = {
    if (id.nonEmpty && id != NoId) {
      body
    }
  }

  def details: Map[(OperatorType, String), ExecutionDetails] = executionDetails.toMap

  def status(operatorType: OperatorType, id: String): Option[ExecutionStatus] = details(operatorType, id).map(_.status)

  def details(operatorType: OperatorType, id: String): Option[ExecutionDetails] =
    executionDetails.get((operatorType, id))

  override def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]                 = started(flow)
  override def flowFinished(flow: ExecutingOperator[Flow]): F[Unit]                = finished(flow)
  override def actionStarted(action: ExecutingOperator[Action]): F[Action]         = started(action)
  override def actionFinished(action: ExecutingOperator[Action]): F[Unit]          = finished(action)
  override def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp]          = started(loop)
  override def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]            = finished(loop)
  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp] = started(forEach)
  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]     = finished(forEach)
  override def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp]             = started(rule)
  override def ruleFinished(rule: ExecutingOperator[RuleOp]): F[Unit]              = finished(rule)
  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp] = started(gateway)
  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp]): F[Unit]     = finished(gateway)
  override def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp]         = started(table)
  override def tableFinished(table: ExecutingOperator[TableOp], executedRows: Int): F[Unit] = {
    ifHasId(table.id) {
      executionDetails((OperatorType.Table, table.id)) = TableExecutionDetails(Finished, executedRows)
    }
    ().pure
  }
  override def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]] =
    started(op)
  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit] = finished(op)

  override def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]] = started(op)
  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit]                 = finished(op)

  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O] = executingOperator.op.pure
  override def executionFinished[O <: FlowOp](
    executingOperator: ExecutingOperator[O],
    error: Option[NdkError]
  ): F[Unit] = ().pure
}

sealed trait ExecutionDetails {
  def status: ExecutionStatus
}
final case class TableExecutionDetails(status: ExecutionStatus, executedRows: Int = 0) extends ExecutionDetails

sealed trait ExecutionStatus extends ExecutionDetails
case object Started extends ExecutionStatus {
  override def status: ExecutionStatus = this
}
case object Finished extends ExecutionStatus {
  override def status: ExecutionStatus = this
}
