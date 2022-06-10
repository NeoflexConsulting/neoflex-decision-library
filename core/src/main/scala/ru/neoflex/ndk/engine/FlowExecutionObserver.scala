package ru.neoflex.ndk.engine

import cats.MonadError
import cats.syntax.applicative._
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.error.NdkError

trait FlowExecutionObserver[F[_]] {
  def flowStarted(flow: Flow): F[Flow]
  def flowFinished(flow: Flow): F[Unit]

  def actionStarted(action: Action): F[Action]
  def actionFinished(action: Action): F[Unit]

  def gatewayStarted(gateway: GatewayOp): F[GatewayOp]
  def gatewayFinished(gateway: GatewayOp): F[Unit]

  def whileStarted(loop: WhileOp): F[WhileOp]
  def whileFinished(loop: WhileOp): F[Unit]

  def forEachStarted(forEach: ForEachOp): F[ForEachOp]
  def forEachFinished(forEach: ForEachOp): F[Unit]

  def ruleStarted(rule: RuleOp): F[RuleOp]
  def ruleFinished(rule: RuleOp): F[Unit]

  def tableStarted(table: TableOp): F[TableOp]
  def tableFinished(table: TableOp, executedRows: Int): F[Unit]

  def pyOperatorStarted(op: PythonOperatorOp[Any, Any]): F[PythonOperatorOp[Any, Any]]
  def pyOperatorFinished(op: PythonOperatorOp[Any, Any]): F[Unit]

  def restServiceStarted(op: RestService[Any, Any]): F[RestService[Any, Any]]
  def restServiceFinished(op: RestService[Any, Any]): F[Unit]
}

class NoOpFlowExecutionObserver[F[_]](implicit monadError: MonadError[F, NdkError]) extends FlowExecutionObserver[F] {
  override def flowStarted(flow: Flow): F[Flow]                                             = flow.pure
  override def flowFinished(flow: Flow): F[Unit]                                            = ().pure
  override def actionStarted(action: Action): F[Action]                                     = action.pure
  override def actionFinished(action: Action): F[Unit]                                      = ().pure
  override def whileStarted(loop: WhileOp): F[WhileOp]                                      = loop.pure
  override def whileFinished(loop: WhileOp): F[Unit]                                        = ().pure
  override def forEachStarted(forEach: ForEachOp): F[ForEachOp]                             = forEach.pure
  override def forEachFinished(forEach: ForEachOp): F[Unit]                                 = ().pure
  override def ruleStarted(rule: RuleOp): F[RuleOp]                                         = rule.pure
  override def ruleFinished(rule: RuleOp): F[Unit]                                          = ().pure
  override def tableStarted(table: TableOp): F[TableOp]                                     = table.pure
  override def tableFinished(table: TableOp, executedRows: Int): F[Unit]                    = ().pure
  override def gatewayStarted(gateway: GatewayOp): F[GatewayOp]                             = gateway.pure
  override def gatewayFinished(gateway: GatewayOp): F[Unit]                                 = ().pure
  override def pyOperatorStarted(op: PythonOperatorOp[Any, Any]): F[PythonOperatorOp[Any, Any]] = op.pure
  override def pyOperatorFinished(op: PythonOperatorOp[Any, Any]): F[Unit]                      = ().pure
  override def restServiceStarted(op: RestService[Any, Any]): F[RestService[Any, Any]] = op.pure
  override def restServiceFinished(op: RestService[Any, Any]): F[Unit]                      = ().pure
}
