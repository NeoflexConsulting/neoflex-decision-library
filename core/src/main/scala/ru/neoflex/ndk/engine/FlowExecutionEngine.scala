package ru.neoflex.ndk.engine

import cats.MonadError
import cats.implicits.toTraverseOps
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.error.{ NdkError, OperatorExecutionError }
import ru.neoflex.ndk.tools.Logging

import scala.util.Try

class FlowExecutionEngine[F[_]](observer: FlowExecutionObserver[F])(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutor[F]
    with Logging {

  override def execute(operator: FlowOp): F[Unit] = executeOperator(operator)

  final protected def executeOperator(flowOp: FlowOp): F[Unit] = flowOp.tailRecM {
    case action: Action     => executeAction(action).map(Either.right)
    case rule: Rule         => executeRule(rule).map(Either.right)
    case table: TableOp     => executeTable(table).map(Either.right)
    case gateway: GatewayOp => findGatewayOperator(gateway).map(Either.left)
    case op: WhileOp        => executeWhile(op).map(Either.right)
    case op: ForEachOp      => executeForEach(op).map(Either.right)
    case flow: Flow         => executeFlow(flow).map(Either.right)
  }

  private def observeExecution[O <: FlowOp, T](
    op: O,
    start: O => F[O],
    finish: O => F[Unit]
  )(
    exec: O => F[T]
  ): F[T] = {
    for {
      tweakedOp <- start(op)
      result    = exec(tweakedOp)
      _         <- finish(tweakedOp)
    } yield result
  }.flatten

  protected def executeFlow(flow: Flow): F[Unit] = {
    logger.debug("Executing flow: ({}, {})", flow.id, flow.name)
    observeExecution(flow, observer.flowStarted, observer.flowFinished) {
      _.ops.map(executeOperator).sequence.map(_ => ())
    }
  }

  protected def executeAction(action: Action): F[Unit] = {
    logger.debug("Executing action: ({}, {})", action.id, action.name)
    observeExecution(action, observer.actionStarted, observer.actionFinished) { tweakedAction =>
      execute(tweakedAction, tweakedAction, tweakedAction.name)
    }
  }

  protected def executeWhile(op: WhileOp): F[Unit] = {
    logger.debug("Executing while loop: ({}, {})", op.id, op.name)
    observeExecution(op, observer.whileStarted, observer.whileFinished) { tweakedWhile =>
      tweakedWhile.tailRecM {
        case While(id, name, condition, body) =>
          logger.trace("Executing while loop[{}, {}] condition", id, name)
          execute(condition, tweakedWhile, name)
            .ifM(
              executeOperator(body).map(_ => Either.left(tweakedWhile)),
              monadError.pure(Either.right(()))
            )
      }
    }
  }

  protected def executeForEach(op: ForEachOp): F[Unit] = {
    logger.debug("Executing forEach loop: ({}, {})", op.id, op.name)
    observeExecution(op, observer.forEachStarted, observer.forEachFinished) { tweakedForEach =>
      tweakedForEach.collection().foldLeft(().pure) { (r, v) =>
        r.flatMap(_ => executeOperator(tweakedForEach.body(v)))
      }
    }
  }

  protected def executeTable(tableIn: TableOp): F[Unit] = new TableExecutor(tableIn, this, observer).execute()

  protected def findGatewayOperator(gatewayIn: GatewayOp): F[FlowOp] =
    observeExecution(gatewayIn, observer.gatewayStarted, observer.gatewayFinished) { gateway =>
      logger.debug("Executing gateway: ({}, {})", gateway.id, gateway.name)
      val conditionsAndExecutionResult = gateway.whens.map(c => Try((c.cond(), c))).toList.sequence
      val foundOperator = for {
        conditionResultAndConditions <- conditionsAndExecutionResult
        foundResultAndCondition      = conditionResultAndConditions.find { case (conditionResult, _) => conditionResult }
        foundTrueCondition           = foundResultAndCondition.map { case (_, condition) => condition }
      } yield foundTrueCondition match {
        case Some(when) =>
          logger.debug(
            "Found first true 'when' within gateway[{}, {}] to execute: {}",
            gateway.id,
            gateway.name,
            when.name
          )
          when.op
        case None =>
          logger.debug(
            "No 'when' branches were found within gateway[{}, {}], the 'otherwise' branch will be selected",
            gateway.id,
            gateway.name
          )
          gateway.otherwise
      }
      pureFromTry(foundOperator, gateway)
    }

  protected def executeRule(rule: Rule): F[Unit] = observeExecution(rule, observer.ruleStarted, observer.ruleFinished) {
    tweakedRule =>
      val Rule(id, name, body) = tweakedRule
      logger.debug("Executing rule: ({}, {})", id, name)

      def executeBranch(f: () => Unit, branchName: String) = {
        logger.trace("Executing {} branch of the rule: ({}, {})", branchName, id, name)
        execute(f, tweakedRule, branchName)
      }

      execute(body.expr, tweakedRule).ifM(
        executeBranch(body.leftBranch, "left"),
        executeBranch(body.rightBranch, "right")
      )
  }

  private[engine] def execute[T](action: () => T, operator: FlowOp, details: Any*): F[T] = {
    pureFromTry(Try(action()), operator, details: _*)
  }

  private def pureFromTry[T](t: Try[T], operator: FlowOp, details: Any*): F[T] = monadError.fromEither {
    t.toEither.leftMap(OperatorExecutionError(operator, _, details: _*))
  }
}
