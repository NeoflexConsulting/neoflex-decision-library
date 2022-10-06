package ru.neoflex.ndk.engine.observer

import cats.MonadError
import cats.syntax.applicative._
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.ExecutingOperator
import ru.neoflex.ndk.error.NdkError

trait FlowExecutionObserver[F[_]] {
  def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O]
  def executionFinished[O <: FlowOp](executingOperator: ExecutingOperator[O], error: Option[NdkError]): F[Unit]

  def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]
  def flowFinished(flow: ExecutingOperator[Flow]): F[Unit]

  def actionStarted(action: ExecutingOperator[Action]): F[Action]
  def actionFinished(action: ExecutingOperator[Action]): F[Unit]

  def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp]
  def gatewayFinished(gateway: ExecutingOperator[GatewayOp], executedBranch: Option[ExecutedBranch]): F[Unit]

  def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp]
  def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]

  def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp]
  def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]

  def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp]
  def ruleFinished(rule: ExecutingOperator[RuleOp], executedBranch: Option[ExecutedBranch]): F[Unit]

  def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp]
  def tableFinished(table: ExecutingOperator[TableOp], executionResult: Option[TableExecutionResult]): F[Unit]

  def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]]
  def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit]

  def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]]
  def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit]
}

class NoOpFlowExecutionObserver[F[_]](implicit monadError: MonadError[F, NdkError]) extends FlowExecutionObserver[F] {
  override def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]                                            = flow.op.pure
  override def flowFinished(flow: ExecutingOperator[Flow]): F[Unit]                                           = ().pure
  override def actionStarted(action: ExecutingOperator[Action]): F[Action]                                    = action.op.pure
  override def actionFinished(action: ExecutingOperator[Action]): F[Unit]                                     = ().pure
  override def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp]                                     = loop.op.pure
  override def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]                                       = ().pure
  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp]                            = forEach.op.pure
  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]                                = ().pure
  override def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp]                                        = rule.op.pure
  override def ruleFinished(rule: ExecutingOperator[RuleOp], executedBranch: Option[ExecutedBranch]): F[Unit] = ().pure
  override def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp]                                    = table.op.pure
  override def tableFinished(
    table: ExecutingOperator[TableOp],
    executionResult: Option[TableExecutionResult]
  ): F[Unit]                                                                       = ().pure
  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp] = gateway.op.pure
  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp], executedBranch: Option[ExecutedBranch]): F[Unit] =
    ().pure
  override def pyOperatorStarted(po: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]] =
    po.op.pure
  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit]              = ().pure
  override def restServiceStarted(svc: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]] = svc.op.pure
  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit]                  = ().pure
  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O]                = executingOperator.op.pure
  override def executionFinished[O <: FlowOp](
    executingOperator: ExecutingOperator[O],
    error: Option[NdkError]
  ): F[Unit] = ().pure
}
