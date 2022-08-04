package ru.neoflex.ndk.testkit

import cats.MonadError
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.{ExecutingOperator, FlowExecutionObserver}
import ru.neoflex.ndk.error.{NdkError, OperatorsTypeMatchError}
import cats.syntax.applicative._
import cats.syntax.applicativeError._

import scala.reflect.ClassTag

class FlowPatchingObserver[F[_]](operatorsToReplace: Map[String, FlowOp])(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutionObserver[F] {

  private def replaceOperatorIfNeeded[O <: FlowOp: ClassTag](execOp: ExecutingOperator[O]): F[O] = {
    operatorsToReplace
      .get(execOp.id)
      .map {
        case replaceOp @ (_: O) => replaceOp.pure
        case replaceOp          => OperatorsTypeMatchError(execOp.op, replaceOp).raiseError[F, O]
      }
      .getOrElse(execOp.op.pure)
  }

  override def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]                          = replaceOperatorIfNeeded(flow)
  override def flowFinished(flow: ExecutingOperator[Flow]): F[Unit]                         = ().pure
  override def actionStarted(action: ExecutingOperator[Action]): F[Action]                  = replaceOperatorIfNeeded(action)
  override def actionFinished(action: ExecutingOperator[Action]): F[Unit]                   = ().pure
  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp]          = replaceOperatorIfNeeded(gateway)
  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp]): F[Unit]              = ().pure
  override def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp]                   = replaceOperatorIfNeeded(loop)
  override def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]                     = ().pure
  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp]          = replaceOperatorIfNeeded(forEach)
  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]              = ().pure
  override def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp]                      = replaceOperatorIfNeeded(rule)
  override def ruleFinished(rule: ExecutingOperator[RuleOp]): F[Unit]                       = ().pure
  override def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp]                  = replaceOperatorIfNeeded(table)
  override def tableFinished(table: ExecutingOperator[TableOp], executedRows: Int): F[Unit] = ().pure

  override def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]] =
    replaceOperatorIfNeeded(op)

  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit] = ().pure

  override def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]] =
    replaceOperatorIfNeeded(op)

  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit] = ().pure

  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O] = executingOperator.op.pure
  override def executionFinished[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[Unit] = ().pure
}
