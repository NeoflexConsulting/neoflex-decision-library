package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.Table.Expression
import ru.neoflex.ndk.dsl._

import scala.annotation.tailrec

object TestExecutor {
  def execute(flow: Flow): Unit = {
    println(s"Executing flow: ${flow.name}")
    flow.ops.foreach(execute)
  }

  @tailrec
  private def execute(op: FlowOp): Unit = op match {
    case a: Action => a()
    case Rule(name, body) =>
      println(s"Executing rule: $name")
      if (body.expr()) {
        body.leftBranch()
      } else {
        body.rightBranch()
      }
    case t: TableOp =>
      println(s"Executing table: ${t.name}")

      val expressionsVal = t.expressions.map {
        case Expression(f, name) =>
          println(s"Executing expression: $name")
          f()
      }

      val actionsMap = t.actions.map(x => (x.name, x)).toMap

      t.conditions.foreach {
        case Table.Condition(operators, action) =>
          val allConditionsSatisfied = expressionsVal.zip(operators).forall { case (v, op) => op(v) }
          if (allConditionsSatisfied) {
            action match {
              case Table.ActionRef(name, args) =>
                actionsMap(name) match {
                  case Table.Action0(_, f) => f()
                  case Table.Action1(_, f) => f(args.value.head)
                  case Table.Action2(_, f) => f(args.value.head, args.value(1))
                  case Table.Action3(_, f) => f(args.value.head, args.value(1), args.value(2))
                  case Table.Action4(_, f) => f(args.value.head, args.value(1), args.value(2), args.value(3))
                  case Table.Action5(_, f) =>
                    f(args.value.head, args.value(1), args.value(2), args.value(3), args.value(4))
                  case Table.Action6(_, _)  => ???
                  case Table.Action7(_, _)  => ???
                  case Table.Action8(_, _)  => ???
                  case Table.Action9(_, _)  => ???
                  case Table.Action10(_, _) => ???
                }

              case Table.SealedAction(f, _) => f()
            }
          }
      }

    case flow: Flow => execute(flow)
    case g: GatewayOp =>
      println(s"Executing gateway: ${g.name}")
      val operator = g.whens.find { when =>
        if (when.cond()) {
          true
        } else {
          false
        }
      }.map(_.op)
        .getOrElse(g.otherwise)

      execute(operator)
  }
}
