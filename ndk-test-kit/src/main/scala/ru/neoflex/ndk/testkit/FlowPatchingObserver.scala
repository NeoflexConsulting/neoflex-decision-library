package ru.neoflex.ndk.testkit

import cats.MonadError
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.FlowExecutionObserver
import ru.neoflex.ndk.error.{ NdkError, OperatorsTypeMatchError }
import cats.syntax.applicative._
import cats.syntax.applicativeError._

import scala.reflect.ClassTag

class FlowPatchingObserver[F[_]](operatorsToReplace: Map[String, FlowOp])(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutionObserver[F] {

  private def replaceOperatorIfNeeded[O <: FlowOp: ClassTag](op: O): F[O] = {
    operatorsToReplace
      .get(op.id)
      .map {
        case replaceOp @ (_: O) => replaceOp.pure
        case replaceOp          => OperatorsTypeMatchError(op, replaceOp).raiseError[F, O]
      }
      .getOrElse(op.pure)
  }

  override def flowStarted(flow: Flow): F[Flow]                          = replaceOperatorIfNeeded(flow)
  override def flowFinished(flow: Flow): F[Unit]                         = ().pure
  override def actionStarted(action: Action): F[Action]                  = replaceOperatorIfNeeded(action)
  override def actionFinished(action: Action): F[Unit]                   = ().pure
  override def gatewayStarted(gateway: GatewayOp): F[GatewayOp]          = replaceOperatorIfNeeded(gateway)
  override def gatewayFinished(gateway: GatewayOp): F[Unit]              = ().pure
  override def whileStarted(loop: WhileOp): F[WhileOp]                   = replaceOperatorIfNeeded(loop)
  override def whileFinished(loop: WhileOp): F[Unit]                     = ().pure
  override def forEachStarted(forEach: ForEachOp): F[ForEachOp]          = replaceOperatorIfNeeded(forEach)
  override def forEachFinished(forEach: ForEachOp): F[Unit]              = ().pure
  override def ruleStarted(rule: RuleOp): F[RuleOp]                      = replaceOperatorIfNeeded(rule)
  override def ruleFinished(rule: RuleOp): F[Unit]                       = ().pure
  override def tableStarted(table: TableOp): F[TableOp]                  = replaceOperatorIfNeeded(table)
  override def tableFinished(table: TableOp, executedRows: Int): F[Unit] = ().pure

  override def pyOperatorStarted(op: PythonOperatorOp[Any, Any]): F[PythonOperatorOp[Any, Any]] =
    replaceOperatorIfNeeded(op)

  override def pyOperatorFinished(op: PythonOperatorOp[Any, Any]): F[Unit] = ().pure
}
