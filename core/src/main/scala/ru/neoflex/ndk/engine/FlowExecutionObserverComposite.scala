package ru.neoflex.ndk.engine

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.flatMap._
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.error.NdkError

class FlowExecutionObserverComposite[F[_]](
  observers: FlowExecutionObserver[F]*
)(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutionObserver[F] {

  private def callObservers[O <: FlowOp](z: O, f: FlowExecutionObserver[F] => O => F[O]): F[O] = {
    observers.foldLeft(z.pure) {
      case (operator, observer) =>
        operator.flatMap(f(observer))
    }
  }

  private def callObservers[O <: FlowOp](f: FlowExecutionObserver[F] => F[Unit]): F[Unit] = {
    observers.foldLeft(().pure) {
      case (r, observer) =>
        r.flatMap(_ => f(observer))
    }
  }

  override def flowStarted(flow: Flow): F[Flow]                 = callObservers(flow, _.flowStarted)
  override def flowFinished(flow: Flow): F[Unit]                = callObservers(_.flowFinished(flow))
  override def actionStarted(action: Action): F[Action]         = callObservers(action, _.actionStarted)
  override def actionFinished(action: Action): F[Unit]          = callObservers(_.actionFinished(action))
  override def gatewayStarted(gateway: GatewayOp): F[GatewayOp] = callObservers(gateway, _.gatewayStarted)
  override def gatewayFinished(gateway: GatewayOp): F[Unit]     = callObservers(_.gatewayFinished(gateway))
  override def whileStarted(loop: WhileOp): F[WhileOp]          = callObservers(loop, _.whileStarted)
  override def whileFinished(loop: WhileOp): F[Unit]            = callObservers(_.whileFinished(loop))
  override def forEachStarted(forEach: ForEachOp): F[ForEachOp] = callObservers(forEach, _.forEachStarted)
  override def forEachFinished(forEach: ForEachOp): F[Unit]     = callObservers(_.forEachFinished(forEach))
  override def ruleStarted(rule: RuleOp): F[RuleOp]             = callObservers(rule, _.ruleStarted)
  override def ruleFinished(rule: RuleOp): F[Unit]              = callObservers(_.ruleFinished(rule))
  override def tableStarted(table: TableOp): F[TableOp]         = callObservers(table, _.tableStarted)
  override def tableFinished(table: TableOp, executedRows: Int): F[Unit] =
    callObservers(_.tableFinished(table, executedRows))
}
