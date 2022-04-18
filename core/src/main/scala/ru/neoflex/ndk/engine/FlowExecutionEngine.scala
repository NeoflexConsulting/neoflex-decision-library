package ru.neoflex.ndk.engine

import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.tools.Logging
import cats.MonadError
import cats.implicits.toTraverseOps
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.either._
import ru.neoflex.ndk.dsl.Table.{ ActionDef, CallableAction }
import ru.neoflex.ndk.error.{
  ActionArgumentsMatchError,
  ExpressionsAndConditionsNumberMatchError,
  NdkError,
  OperatorExecutionError,
  TableActionNotFound
}

import scala.util.Try

class FlowExecutionEngine[F[_]](implicit monadError: MonadError[F, NdkError]) extends FlowExecutor[F] with Logging {

  override def execute(flow: Flow): F[Unit] = {
    logger.debug("Executing flow: {}", flow.name)
    flow.ops.map(executeOperator).toList.sequence.map(_ => ())
  }

  final protected def executeOperator(flowOp: FlowOp): F[Unit] = monadError.tailRecM(flowOp) {
    case action: Action     => executeAction(action).map(Either.right)
    case rule: Rule         => executeRule(rule).map(Either.right)
    case table: TableOp     => executeTable(table).map(Either.right)
    case gateway: GatewayOp => findGatewayOperator(gateway).map(Either.left)
    case op: WhileOp        => executeWhile(op).map(Either.right)
    case op: IterateOp      => executeIterate(op).map(Either.right)
    case flow: Flow         => execute(flow).map(Either.right)
  }

  protected def executeAction(action: Action): F[Unit] = {
    logger.debug("Executing action: {}", action.name)
    execute(action, action, action.name)
  }

  protected def executeWhile(op: WhileOp): F[Unit] = {
    logger.debug("Executing while loop: {}", op.name)
    monadError.tailRecM(op) {
      case While(name, condition, body) =>
        logger.trace("Executing while loop[{}] condition", op.name)
        execute(condition, op, name).flatMap { conditionResult =>
          if (conditionResult) {
            executeOperator(body).map(_ => Either.left(op))
          } else monadError.pure(Either.right(()))
        }
    }
  }

  protected def executeIterate(op: IterateOp): F[Unit] = {
    logger.debug("Executing iterate loop: {}", op.name)
    executeOperator(op.body)
  }

  protected def executeTable(table: TableOp): F[Unit] = {
    logger.debug(s"Executing table: ${table.name}")

    val tableActionDefs = table.actions.map(a => (a.name, a)).toMap

    def executeExpressions(expressions: Seq[Table.Expression]) =
      expressions.map { expr =>
        execute(expr.f, table, expr.name).map((expr.name, _))
      }.toList.sequence

    def executeActionDef(action: ActionDef, args: Table.Args) = {
      val function = action match {
        case Table.Action0(_, f) => f
        case Table.Action1(_, f) => () => f(args(0))
        case Table.Action2(_, f) => () => f(args(0), args(1))
        case Table.Action3(_, f) => () => f(args(0), args(1), args(2))
        case Table.Action4(_, f) => () => f(args(0), args(1), args(2), args(3))
        case Table.Action5(_, f) => () => f(args(0), args(1), args(2), args(3), args(4))
        case Table.Action6(_, f) => () => f(args(0), args(1), args(2), args(3), args(4), args(5))
        case Table.Action7(_, f) => () => f(args(0), args(1), args(2), args(3), args(4), args(5), args(6))
        case Table.Action8(_, f) => () => f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7))
        case Table.Action9(_, f) =>
          () => f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8))
        case Table.Action10(_, f) =>
          () => f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9))
      }
      logger.debug("Executing table[{}] action def: {}", table.name, action.name)
      execute(function, table, action, args)
    }

    def executeAction(action: CallableAction): F[Unit] = action match {
      case Table.ActionRef(name, args) =>
        for {
          actionDef <- monadError.fromOption(tableActionDefs.get(name), TableActionNotFound(table, name))
          _ <- if (actionDef.argsCount != args.count) {
                monadError.raiseError(ActionArgumentsMatchError(table, actionDef, args))
              } else {
                ().pure
              }
          _ <- executeActionDef(actionDef, args)
        } yield ()
      case Table.SealedAction(f, name) =>
        logger.debug("Executing table[{}] action: {}", table.name, name)
        execute(f, table, name)
    }

    def executeActionIf(conditionResult: Boolean, condition: Table.Condition) = {
      if (conditionResult) {
        executeAction(condition.callableAction)
      } else {
        ().pure
      }
    }

    def checkCondition(expressionResults: List[(String, Any)], condition: Table.Condition) = {
      expressionResults
        .zip(condition.operators)
        .map {
          case ((exprName, exprValue), operator) =>
            execute(() => operator(exprValue), table, exprName, operator)
        }
        .sequence
        .map(_.forall(x => x))
    }

    def checkExpressionsAndConditionsNumber() = {
      table.conditions
        .map(c => (c.operators.length == table.expressions.length, c))
        .foldLeft(().pure) {
          case (finalResult, (result, c)) =>
            monadError.ensure(finalResult)(ExpressionsAndConditionsNumberMatchError(table, c))(_ => result)
        }
    }

    for {
      _                 <- checkExpressionsAndConditionsNumber()
      expressionResults <- executeExpressions(table.expressions)
      conditionsResults <- table.conditions.map(c => checkCondition(expressionResults, c).map((_, c))).sequence
      _                 <- conditionsResults.map { case (result, condition) => executeActionIf(result, condition) }.sequence
    } yield ()
  }

  protected def findGatewayOperator(gateway: GatewayOp): F[FlowOp] = {
    logger.debug("Executing gateway: {}", gateway.name)
    val conditionsAndExecutionResult = gateway.whens.map(c => Try((c.cond(), c))).toList.sequence
    val foundOperator = for {
      conditionResultAndConditions <- conditionsAndExecutionResult
      foundResultAndCondition      = conditionResultAndConditions.find { case (conditionResult, _) => conditionResult }
      foundTrueCondition           = foundResultAndCondition.map { case (_, condition) => condition }
    } yield foundTrueCondition match {
      case Some(when) =>
        logger.debug(s"Found first true 'when' within gateway[${gateway.name}] to execute: {}", when.name)
        when.op
      case None =>
        logger.debug(
          s"No 'when' branches were found within gateway[${gateway.name}], the 'otherwise' branch will be selected"
        )
        gateway.otherwise
    }
    pureFromTry(foundOperator, gateway)
  }

  protected def executeRule(rule: Rule): F[Unit] = {
    val Rule(name, body) = rule
    logger.debug("Executing rule: {}", name)

    execute(body.expr, rule).flatMap { conditionResult =>
      if (conditionResult) {
        logger.trace("Executing left branch of the rule: {}", name)
        execute(body.leftBranch, rule)
      } else {
        logger.trace("Executing right branch of the rule: {}", name)
        execute(body.rightBranch, rule)
      }
    }
  }

  private def execute[T](action: () => T, operator: FlowOp, details: Any*): F[T] = {
    pureFromTry(Try(action()), operator, details: _*)
  }

  private def pureFromTry[T](t: Try[T], operator: FlowOp, details: Any*): F[T] = monadError.fromEither {
    t.toEither.leftMap(OperatorExecutionError(operator, _, details: _*))
  }
}
