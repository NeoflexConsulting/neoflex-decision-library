package ru.neoflex.ndk.engine

import cats.implicits.{ catsSyntaxOptionId, none, toTraverseOps }
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{ ~>, MonadError }
import ru.neoflex.ndk.dsl.RestServiceImplicits.RestServiceOps
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.dictionary._
import ru.neoflex.ndk.dsl.syntax.NoName
import ru.neoflex.ndk.engine.ExecutingOperator.Ops
import ru.neoflex.ndk.engine.observer.{ DictConditionDetails, ExecutedBranch, FlowExecutionObserver }
import ru.neoflex.ndk.engine.process.{ PooledProcess, ProcessPool }
import ru.neoflex.ndk.error._
import ru.neoflex.ndk.tools.Logging
import ru.neoflex.ndk.{ ExecutionConfig, RestConfig }

import scala.util.Using.Releasable
import scala.util.{ Try, Using }

class FlowExecutionEngine[F[_], G[_]](
  observer: FlowExecutionObserver[G],
  executionConfig: ExecutionConfig,
  processPool: ProcessPool,
  liftG: G ~> F
)(implicit monadError: MonadError[F, NdkError])
    extends FlowExecutor[F]
    with Logging {

  override def execute(operator: FlowOp): F[Unit] = {
    val executionResult = for {
      tweakedOp <- liftG(observer.executionStarted(ExecutingOperator(operator, operator)))
      _         <- executeOperator(tweakedOp, tweakedOp)
      _         <- liftG(observer.executionFinished(ExecutingOperator(tweakedOp, tweakedOp), None))
    } yield ()

    executionResult.onError {
      case e =>
        liftG {
          observer.executionFinished(ExecutingOperator(operator, operator), e.some)
        }
    }
  }

  final protected def executeOperator(flowOp: FlowOp, root: FlowOp): F[Unit] = flowOp.tailRecM {
    case action: Action     => executeAction(action.withParent(root, flowOp)).map(Either.right)
    case rule: RuleOp       => executeRule(rule.withParent(root, flowOp)).map(Either.right)
    case table: TableOp     => executeTable(table.withParent(root, flowOp)).map(Either.right)
    case gateway: GatewayOp => findGatewayOperator(gateway.withParent(root, flowOp)).map(Either.left)
    case op: WhileOp        => executeWhile(op.withParent(root, flowOp)).map(Either.right)
    case op: ForEachOp      => executeForEach(op.withParent(root, flowOp)).map(Either.right)
    case flow: Flow         => executeFlow(flow.withParent(root, flowOp)).map(Either.right)
    case op: PythonOperatorOp[_, _] =>
      executePythonOperator(op.asInstanceOf[PythonOperatorOp[Any, Any]].withParent(root, flowOp)).map(Either.right)
    case service: RestService[_, _] =>
      callRemoteService(service.asInstanceOf[RestService[Any, Any]].withParent(root, flowOp)).map(Either.right)
  }

  private def observeExecution[O <: FlowOp, T](
    op: ExecutingOperator[O],
    start: ExecutingOperator[O] => G[O],
    finish: ExecutingOperator[O] => G[Unit]
  )(
    exec: O => F[T]
  ): F[T] = {
    for {
      tweakedOp <- liftG(start(op))
      result    = exec(tweakedOp)
      _         <- liftG(finish(tweakedOp.withParentFrom(op)))
    } yield result
  }.flatten

  protected def executePythonOperator(pyOperatorIn: ExecutingOperator[PythonOperatorOp[Any, Any]]): F[Unit] = {
    logger.debug("Executing python operator: ({}, {})", pyOperatorIn.id, pyOperatorIn.name)
    implicit val pooledProcessIsReleasable: Releasable[PooledProcess] = processPool.release(_)

    def callProcess(pooledProcess: PooledProcess, op: PythonOperatorOp[Any, Any]) = {
      Using(pooledProcess) { p =>
        val processWriter = p.getProcessWriter
        val processReader = p.getProcessReader
        for {
          processInput  <- pyOperatorIn.op.encodedDataIn.leftMap(PyDataEncodeError(op, _))
          _             <- processWriter.writeData(processInput).toEither.leftMap(PyOperatorWritingError(op, _))
          processOutput <- processReader.readSingleData().toEither.leftMap(OperatorExecutionError(op, _))
          _             <- op.collectResults(processOutput).leftMap(PyDataDecodeError(processOutput, op, _))
        } yield ()
      }.toEither.leftMap[NdkError](OperatorExecutionError(op, _)).joinRight
    }

    observeExecution(pyOperatorIn, observer.pyOperatorStarted, observer.pyOperatorFinished) { op =>
      for {
        pooledProcess <- processPool
                          .borrowProcess("python", op.command)
                          .toEither
                          .leftMap(PyOperatorStartError(op, _))
                          .liftTo[F]
        _ <- callProcess(pooledProcess, op).liftTo[F]
      } yield ()
    }
  }

  private def callRemoteService(rs: ExecutingOperator[RestService[Any, Any]]): F[Unit] = {
    observeExecution(rs, observer.restServiceStarted, observer.restServiceFinished) { op =>
      implicit val restConfig: RestConfig = executionConfig.rest
      op.executeRequest()
    }
  }

  protected def executeFlow(flow: ExecutingOperator[Flow]): F[Unit] = {
    logger.debug("Executing flow: ({}, {})", flow.id, flow.name)
    observeExecution(flow, observer.flowStarted, observer.flowFinished) {
      _.ops.map(executeOperator(_, flow.root)).sequence.map(_ => ())
    }
  }

  protected def executeAction(action: ExecutingOperator[Action]): F[Unit] = {
    logger.debug("Executing action: ({}, {})", action.id, action.name)
    observeExecution(action, observer.actionStarted, observer.actionFinished) { tweakedAction =>
      execute(tweakedAction, tweakedAction, tweakedAction.name)
    }
  }

  protected def executeWhile(op: ExecutingOperator[WhileOp]): F[Unit] = {
    logger.debug("Executing while loop: ({}, {})", op.id, op.name)
    observeExecution(op, observer.whileStarted, observer.whileFinished) { tweakedWhile =>
      tweakedWhile.tailRecM {
        case While(id, name, condition, body) =>
          logger.trace("Executing while loop[{}, {}] condition", id, name)
          execute(condition, tweakedWhile, name)
            .ifM(
              executeOperator(body, op.root).map(_ => Either.left(tweakedWhile)),
              monadError.pure(Either.right(()))
            )
        case o => throw new MatchError(o)
      }
    }
  }

  protected def executeForEach(op: ExecutingOperator[ForEachOp]): F[Unit] = {
    logger.debug("Executing forEach loop: ({}, {})", op.id, op.name)
    observeExecution(op, observer.forEachStarted, observer.forEachFinished) { tweakedForEach =>
      tweakedForEach.collection().foldLeft(().pure[F]) { (r, v) =>
        r.flatMap(_ => executeOperator(tweakedForEach.body(v), op.root))
      }
    }
  }

  protected def executeTable(tableIn: ExecutingOperator[TableOp]): F[Unit] =
    new TableExecutor[F, G](tableIn, this, observer, liftG).execute()

  protected def findGatewayOperator(gatewayIn: ExecutingOperator[GatewayOp]): F[FlowOp] =
    liftG(observer.gatewayStarted(gatewayIn)).flatMap { gateway =>
      logger.debug("Executing gateway: ({}, {})", gateway.id, gateway.name)
      val conditionsAndExecutionResult = gateway.whens
        .map(c => c.cond.eval().map(r => (r, c)))
        .toList
        .sequence
        .leftMap(e => OperatorExecutionError(gateway, None, e))
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
          val executedBranch = ExecutedBranch(when.name.getOrElse(NoName), extractConditionDetails(when.cond.some))
          (when.op, executedBranch)
        case None =>
          logger.debug(
            "No 'when' branches were found within gateway[{}, {}], the 'otherwise' branch will be selected",
            gateway.id,
            gateway.name
          )
          val executedBranch = ExecutedBranch("otherwise", None)
          (gateway.otherwise, executedBranch)
      }
      foundOperator.liftTo[F].flatMap {
        case (op, executedBranch) =>
          liftG(observer.gatewayFinished(gateway.withParentFrom(gatewayIn), executedBranch.some)).map(_ => op)
      }
    }

  private def extractConditionDetails(condition: Option[LazyCondition]) = condition.flatMap { c =>
    c match {
      case DictLazyCondition(v, cond) =>
        val left = v.get.toOption.flatten.map(_.toString).getOrElse("")
        val conditionView =
          cond match {
            case EqCondExpression(right)   => s"$left == $right"
            case GtCondExpression(right)   => s"$left > $right"
            case LtCondExpression(right)   => s"$left < $right"
            case GtEqCondExpression(right) => s"$left >= $right"
            case LtEqCondExpression(right) => s"$left <= $right"
          }
        DictConditionDetails(v.dictionaryName, v.key, conditionView).some
      case _ => none
    }
  }

  protected def executeRule(rule: ExecutingOperator[RuleOp]): F[Unit] = {
    liftG(observer.ruleStarted(rule)).flatMap { tweakedRule =>
      import tweakedRule._
      logger.debug("Executing rule: ({}, {})", id, name)

      def executeBranch(conditionOrOtherwise: Either[Rule.Condition, Option[Rule.Otherwise]]) = {
        conditionOrOtherwise.left.map { c =>
          NamedAction(c.name.getOrElse(NoName), c.body, c.expr.some).some
        }.map { otherwise =>
          otherwise.map { o =>
            NamedAction(o.name.getOrElse("otherwise"), o.body, none)
          }
        }.fold(a1 => a1, a2 => a2)
          .map { r =>
            logger.trace("Executing {} branch of the rule: ({}, {})", r.name, id, name)
            execute(r.body, tweakedRule, r.name).map { _ =>
              ExecutedBranch(r.name, extractConditionDetails(r.condition)).some
            }
          }
          .getOrElse(none[ExecutedBranch].pure)
      }

      def executeConditions() =
        conditions
          .map(c => c.expr.eval().map(r => (c, r)))
          .toList
          .sequence
          .leftMap(e => OperatorExecutionError(tweakedRule, None, e))
          .liftTo[F]

      for {
        conditionAndResult  <- executeConditions()
        maybeTrueCondition  = conditionAndResult.find(_._2).map(_._1)
        trueCondOrOtherwise = maybeTrueCondition.toLeft(otherwise)
        executedBranch      <- executeBranch(trueCondOrOtherwise)
        _                   <- liftG(observer.ruleFinished(tweakedRule.withParentFrom(rule), executedBranch))
      } yield ()
    }
  }

  private[engine] def execute[T](action: () => T, operator: FlowOp, details: Any*): F[T] = {
    pureFromTry(Try(action()), operator, details: _*)
  }

  private def pureFromTry[T](t: Try[T], operator: FlowOp, details: Any*): F[T] = monadError.fromEither {
    t.toEither.leftMap(OperatorExecutionError(operator, _, details: _*))
  }
}

private[engine] final case class NamedAction(name: String, body: () => Unit, condition: Option[LazyCondition])
