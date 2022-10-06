package ru.neoflex.ndk.engine.observer

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.ExecutingOperator
import ru.neoflex.ndk.engine.ExecutingOperator.Ops
import ru.neoflex.ndk.error.NdkError

class FlowExecutionObserverComposite[F[_]](
  observers: FlowExecutionObserver[F]*
)(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutionObserver[F] {

  private def callObservers[O <: FlowOp](z: ExecutingOperator[O], f: FlowExecutionObserver[F] => ExecutingOperator[O] => F[O]): F[O] = {
    observers.foldLeft(z.pure) {
      case (operator, observer) =>
        operator.flatMap(f(observer)).map(o => o.withParentFrom(z))
    }.map(_.op)
  }

  private def callObservers[O <: FlowOp](f: FlowExecutionObserver[F] => F[Unit]): F[Unit] = {
    observers.foldLeft(().pure) {
      case (r, observer) =>
        r.flatMap(_ => f(observer))
    }
  }

  override def flowStarted(flow: ExecutingOperator[Flow]): F[Flow]                 = callObservers(flow, _.flowStarted)
  override def flowFinished(flow: ExecutingOperator[Flow]): F[Unit]                = callObservers(_.flowFinished(flow))
  override def actionStarted(action: ExecutingOperator[Action]): F[Action]         = callObservers(action, _.actionStarted)
  override def actionFinished(action: ExecutingOperator[Action]): F[Unit]          = callObservers(_.actionFinished(action))
  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): F[GatewayOp] = callObservers(gateway, _.gatewayStarted)
  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp], executedBranch: Option[ExecutedBranch]): F[Unit]     =
    callObservers(_.gatewayFinished(gateway, executedBranch))

  override def whileStarted(loop: ExecutingOperator[WhileOp]): F[WhileOp]          = callObservers(loop, _.whileStarted)
  override def whileFinished(loop: ExecutingOperator[WhileOp]): F[Unit]            = callObservers(_.whileFinished(loop))
  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): F[ForEachOp] = callObservers(forEach, _.forEachStarted)
  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): F[Unit]     = callObservers(_.forEachFinished(forEach))
  override def ruleStarted(rule: ExecutingOperator[RuleOp]): F[RuleOp]             = callObservers(rule, _.ruleStarted)
  override def ruleFinished(rule: ExecutingOperator[RuleOp], executedBranch: Option[ExecutedBranch]): F[Unit]              =
    callObservers(_.ruleFinished(rule, executedBranch))

  override def tableStarted(table: ExecutingOperator[TableOp]): F[TableOp]         = callObservers(table, _.tableStarted)
  override def tableFinished(table: ExecutingOperator[TableOp], executionResult: Option[TableExecutionResult]): F[Unit] =
    callObservers(_.tableFinished(table, executionResult))

  override def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[PythonOperatorOp[Any, Any]] =
    callObservers(op, _.pyOperatorStarted)

  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit] = callObservers(_.pyOperatorFinished(op))

  override def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): F[RestService[Any, Any]] =
    callObservers(op, _.restServiceStarted)

  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): F[Unit] = callObservers(_.restServiceFinished(op))

  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): F[O] = callObservers(executingOperator, _.executionStarted)

  override def executionFinished[O <: FlowOp](executingOperator: ExecutingOperator[O], error: Option[NdkError]): F[Unit] = callObservers(_.executionFinished(executingOperator, error))
}
