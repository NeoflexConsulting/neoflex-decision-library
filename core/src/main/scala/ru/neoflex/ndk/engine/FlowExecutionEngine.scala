package ru.neoflex.ndk.engine

import cats.MonadError
import cats.implicits.{ catsSyntaxOptionId, toTraverseOps }
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.neoflex.ndk.dsl.RestServiceImplicits.RestServiceOps
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.syntax.NoName
import ru.neoflex.ndk.error._
import ru.neoflex.ndk.tools.Logging
import ru.neoflex.ndk.{ ExecutionConfig, RestConfig }

import java.io.PrintWriter
import scala.io.Source
import scala.util.Try

class FlowExecutionEngine[F[_]](
  observer: FlowExecutionObserver[F],
  executionConfig: ExecutionConfig
)(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutor[F]
    with Logging {

  override def execute(operator: FlowOp): F[Unit] = executeOperator(operator)

  final protected def executeOperator(flowOp: FlowOp): F[Unit] = flowOp.tailRecM {
    case action: Action     => executeAction(action).map(Either.right)
    case rule: RuleOp       => executeRule(rule).map(Either.right)
    case table: TableOp     => executeTable(table).map(Either.right)
    case gateway: GatewayOp => findGatewayOperator(gateway).map(Either.left)
    case op: WhileOp        => executeWhile(op).map(Either.right)
    case op: ForEachOp      => executeForEach(op).map(Either.right)
    case flow: Flow         => executeFlow(flow).map(Either.right)
    case op: PythonOperatorOp[_, _] =>
      executePythonOperator(op.asInstanceOf[PythonOperatorOp[Any, Any]]).map(Either.right)
    case service: RestService[_, _] =>
      callRemoteService(service.asInstanceOf[RestService[Any, Any]]).map(Either.right)
  }

  private def observeExecution[O <: FlowOp, T](op: O, start: O => F[O], finish: O => F[Unit])(exec: O => F[T]): F[T] = {
    for {
      tweakedOp <- start(op)
      result    = exec(tweakedOp)
      _         <- finish(tweakedOp)
    } yield result
  }.flatten

  protected def executePythonOperator(pyOperatorIn: PythonOperatorOp[Any, Any]): F[Unit] = {
    def writeInputData(p: Process, o: PythonOperatorOp[_, _]): F[Unit] = {
      val dataInWriter = new PrintWriter(p.getOutputStream)
      o.encodedDataIn.foreach { v =>
        dataInWriter.println(v)
      }
      dataInWriter.flush()
      dataInWriter.close()
      monadError.raiseWhen(dataInWriter.checkError())(PyOperatorWritingError(o))
    }

    logger.debug("Executing python operator: ({}, {})", pyOperatorIn.id, pyOperatorIn.name)

    observeExecution(pyOperatorIn, observer.pyOperatorStarted, observer.pyOperatorFinished) { op =>
      val pb = new ProcessBuilder("python", op.command).redirectErrorStream(true)

      for {
        process  <- Try(pb.start()).toEither.leftMap(PyOperatorStartError(op, _)).liftTo[F]
        _        <- writeInputData(process, op)
        exitCode = process.waitFor()
        _        <- monadError.raiseWhen(exitCode != 0)(PyOperatorExecutionError(op, exitCode))
        output   = Source.fromInputStream(process.getInputStream).getLines().toSeq
        _        <- Try(process.getInputStream.close()).toEither.leftMap(OperatorExecutionError(op, _)).liftTo[F]
        _        <- op.collectResults(output).leftMap(PyDataDecodeError(output, op, _)).liftTo[F]
      } yield ()
    }
  }

  private def callRemoteService(rs: RestService[Any, Any]): F[Unit] = {
    observeExecution(rs, observer.restServiceStarted, observer.restServiceFinished) { op =>
      implicit val restConfig: RestConfig = executionConfig.rest
      op.executeRequest()
    }
  }

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
        case o => throw new MatchError(o)
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

  protected def executeRule(rule: RuleOp): F[Unit] =
    observeExecution(rule, observer.ruleStarted, observer.ruleFinished) { tweakedRule =>
      import tweakedRule._
      logger.debug("Executing rule: ({}, {})", id, name)

      def executeBranch(r: NamedAction) = {
        logger.trace("Executing {} branch of the rule: ({}, {})", r.name, id, name)
        execute(r.body, tweakedRule, r.name)
      }

      for {
        conditionAndResult <- pureFromTry(conditions.map { c =>
                               Try((c, c.expr()))
                             }.toList.sequence, tweakedRule)
        maybeTrueCondition   = conditionAndResult.find(_._2).map(_._1)
        maybeConditionAction = maybeTrueCondition.map(c => NamedAction(c.name.getOrElse(NoName), c.body)).map(_.some)
        maybeRuleAction = maybeConditionAction.getOrElse {
          otherwise.map(o => NamedAction(o.name.getOrElse(NoName), o.body))
        }
        _ <- maybeRuleAction.map(executeBranch).getOrElse(().pure)
      } yield ()
    }

  private[engine] def execute[T](action: () => T, operator: FlowOp, details: Any*): F[T] = {
    pureFromTry(Try(action()), operator, details: _*)
  }

  private def pureFromTry[T](t: Try[T], operator: FlowOp, details: Any*): F[T] = monadError.fromEither {
    t.toEither.leftMap(OperatorExecutionError(operator, _, details: _*))
  }
}

private[engine] final case class NamedAction(name: String, body: () => Unit)
