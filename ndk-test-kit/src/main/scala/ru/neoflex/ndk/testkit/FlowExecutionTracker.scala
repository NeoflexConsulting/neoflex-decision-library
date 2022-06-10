package ru.neoflex.ndk.testkit

import cats.MonadError
import cats.syntax.applicative._
import ru.neoflex.ndk.dsl.syntax.NoId
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.FlowExecutionObserver
import ru.neoflex.ndk.error.NdkError

class FlowExecutionTracker[F[_]](implicit monadError: MonadError[F, NdkError]) extends FlowExecutionObserver[F] {
  private val executionDetails = collection.mutable.Map[(OperatorType, String), ExecutionDetails]()

  private def started[O <: FlowOp](op: O): F[O] = {
    ifHasId(op.id) {
      executionDetails((OperatorType(op), op.id)) = Started
    }
    op.pure
  }

  private def finished[O <: FlowOp](op: O): F[Unit] = {
    ifHasId(op.id) {
      executionDetails((OperatorType(op), op.id)) = Finished
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

  override def flowStarted(flow: Flow): F[Flow]                 = started(flow)
  override def flowFinished(flow: Flow): F[Unit]                = finished(flow)
  override def actionStarted(action: Action): F[Action]         = started(action)
  override def actionFinished(action: Action): F[Unit]          = finished(action)
  override def whileStarted(loop: WhileOp): F[WhileOp]          = started(loop)
  override def whileFinished(loop: WhileOp): F[Unit]            = finished(loop)
  override def forEachStarted(forEach: ForEachOp): F[ForEachOp] = started(forEach)
  override def forEachFinished(forEach: ForEachOp): F[Unit]     = finished(forEach)
  override def ruleStarted(rule: RuleOp): F[RuleOp]             = started(rule)
  override def ruleFinished(rule: RuleOp): F[Unit]              = finished(rule)
  override def gatewayStarted(gateway: GatewayOp): F[GatewayOp] = started(gateway)
  override def gatewayFinished(gateway: GatewayOp): F[Unit]     = finished(gateway)
  override def tableStarted(table: TableOp): F[TableOp]         = started(table)
  override def tableFinished(table: TableOp, executedRows: Int): F[Unit] = {
    ifHasId(table.id) {
      executionDetails((OperatorType.Table, table.id)) = TableExecutionDetails(Finished, executedRows)
    }
    ().pure
  }
  override def pyOperatorStarted(op: PythonOperatorOp[Any, Any]): F[PythonOperatorOp[Any, Any]] = started(op)
  override def pyOperatorFinished(op: PythonOperatorOp[Any, Any]): F[Unit]                      = finished(op)

  override def restServiceStarted(op: RestService[Any, Any]): F[RestService[Any, Any]] = started(op)
  override def restServiceFinished(op: RestService[Any, Any]): F[Unit]                 = finished(op)
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
}
