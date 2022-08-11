package ru.neoflex.ndk.engine.tracking

import cats.MonadError
import cats.implicits.catsSyntaxApplicativeId
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
import ru.neoflex.ndk.engine.{ ExecutingOperator, FlowExecutionObserver }
import ru.neoflex.ndk.error.NdkError

final class FlowTrackingObserver[F[_]](
  tracker: FlowTracker,
  eventHandler: OperatorTrackedEventRoot => F[Unit]
)(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutionObserver[F] {

  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O] =
    tracker.executionStarted(executingOperator).pure

  override def executionFinished[O <: FlowOp](
    executingOperator: ExecutingOperator[O],
    error: Option[NdkError]
  ): F[Unit] = {
    tracker.executionFinished(executingOperator, error).map(eventHandler).getOrElse(().pure)
  }

  override def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]  = tracker.started(flow).pure
  override def flowFinished(flow: ExecutingOperator[Flow]): F[Unit] = tracker.finished(flow).pure

  override def actionStarted(action: ExecutingOperator[Action]): F[Action] = tracker.started(action).pure
  override def actionFinished(action: ExecutingOperator[Action]): F[Unit]  = tracker.finished(action).pure

  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp] = tracker.started(gateway).pure
  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp]): F[Unit]     = tracker.finished(gateway).pure

  override def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp] = tracker.started(loop).pure
  override def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]   = tracker.finished(loop).pure

  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp] = tracker.started(forEach).pure
  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]     = tracker.finished(forEach).pure

  override def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp] = tracker.started(rule).pure
  override def ruleFinished(rule: ExecutingOperator[RuleOp]): F[Unit]  = tracker.finished(rule).pure

  override def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp] = tracker.started(table).pure
  override def tableFinished(table: ExecutingOperator[TableOp], executedRows: Int): F[Unit] =
    tracker.finished(table).pure

  override def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]] =
    tracker.started(op).pure
  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit] =
    tracker.finished(op).pure

  override def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]] =
    tracker.started(op).pure
  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit] = tracker.finished(op).pure
}
