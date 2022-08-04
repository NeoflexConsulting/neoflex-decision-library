package ru.neoflex.ndk.engine

import cats.syntax.either._
import cats.{MonadError, ~>}
import cats.implicits.toTraverseOps
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.neoflex.ndk.dsl.Table.{ActionDef, CallableAction}
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.dictionary.DictionaryValue
import ru.neoflex.ndk.engine.ExecutingOperator.Ops
import ru.neoflex.ndk.error.{ActionArgumentsMatchError, ExpressionsAndConditionsNumberMatchError, NdkError, TableActionNotFound}
import ru.neoflex.ndk.tools.Logging

class TableExecutor[F[_], G[_]](
  tableIn: ExecutingOperator[TableOp],
  engine: FlowExecutionEngine[F, G],
  observer: FlowExecutionObserver[G],
  liftG: G ~> F
)(implicit monadError: MonadError[F, NdkError])
    extends Logging {
  private val _table = liftG(observer.tableStarted(tableIn))

  def execute(): F[Unit] = {
    val result = for {
      table        <- _table
      rowsExecuted <- execute(table)
      _            <- liftG(observer.tableFinished(table.withParentFrom(tableIn), rowsExecuted))
    } yield ()

    result.onError {
      case _ => _table.flatMap(x => liftG(observer.tableFinished(x.withParentFrom(tableIn), 0)))
    }
  }

  private def execute(table: TableOp): F[Int] = {
    logger.debug("Executing table: ({}, {})", table.id, table.name)
    for {
      _                 <- checkExpressionsAndConditionsNumber()
      expressionResults <- executeExpressions(table.expressions)
      conditionsResults <- table.conditions.map(c => checkCondition(expressionResults, c).map((_, c))).sequence
      executedRows      <- conditionsResults.map { case (r, c) => executeActionIf(r, c) }.sequence.map(_.sum)
    } yield executedRows
  }

  private def executeExpressions(expressions: Seq[Table.Expression]) = _table.flatMap { table =>
    expressions.map { expr =>
      engine
        .execute(expr.f, table, expr.name)
        .flatMap {
          case dv: DictionaryValue[_] => dv.get.liftTo[F].asInstanceOf[F[Any]]
          case r                      => r.pure[F]
        }
        .map((expr.name, _))
    }.toList.sequence
  }

  private def executeActionDef(action: ActionDef, args: Table.Args) = _table.flatMap { table =>
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
    logger.debug("Executing table[{}, {}] action def: {}", table.id, table.name, action.name)
    engine.execute(function, table, action, args)
  }

  private def executeActionIf(conditionResult: Boolean, condition: Table.Condition) = {
    if (conditionResult) {
      executeAction(condition.callableAction).map(_ => 1)
    } else {
      0.pure[F]
    }
  }

  private def executeAction(action: CallableAction): F[Unit] = _table.flatMap { table =>
    action match {
      case Table.ActionRef(name, args) =>
        for {
          actionDef <- monadError.fromOption(table.actionsByName.get(name), TableActionNotFound(table, name))
          _ <- if (actionDef.argsCount != args.count) {
                monadError.raiseError(ActionArgumentsMatchError(table, actionDef, args))
              } else {
                ().pure[F]
              }
          _ <- executeActionDef(actionDef, args)
        } yield ()
      case Table.SealedAction(f, name) =>
        logger.debug("Executing table[{}, {}] action[{}]", table.id, table.name, name)
        engine.execute(f, table, name)
    }
  }

  private def checkCondition(expressionResults: List[(String, Any)], condition: Table.Condition) = _table.flatMap {
    table =>
      expressionResults
        .zip(condition.operators)
        .map {
          case ((exprName, exprValue), operator) =>
            engine.execute(() => operator(exprValue), table, exprName, operator)
        }
        .sequence
        .map(_.forall(x => x))
  }

  private def checkExpressionsAndConditionsNumber() = _table.flatMap { table =>
    table.conditions
      .map(c => (c.operators.length == table.expressions.length, c))
      .foldLeft(().pure[F]) {
        case (finalResult, (result, c)) =>
          monadError.ensure(finalResult)(ExpressionsAndConditionsNumberMatchError(table, c))(_ => result)
      }
  }
}
