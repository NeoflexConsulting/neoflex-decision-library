package ru.neoflex.ndk.engine.tracking

import cats.Id
import cats.implicits.catsSyntaxFlatMapIdOps
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.error.NdkError

final case class Cursor(previous: Option[Cursor], op: TrackingOperator)

final case class TrackingOperatorCursor(rootOperator: FlowOp, cursor: Cursor)

object TrackingOperatorCursor {
  implicit def toEvent(tracker: TrackingOperatorCursor, error: Option[NdkError]): OperatorTrackedEventRoot = {
    def toEvent(to: TrackingOperator): OperatorTrackedEvent = {
      val operator = to.op
      val children = to.children.map { to =>
        toEvent(to)
      }
      OperatorTrackedEvent(
        operator.id,
        operator.name,
        operator.operatorType,
        to.runAt.toEpochMilli,
        to.finishedAt.map(_.toEpochMilli),
        children
      )
    }

    val rootCursor = tracker.cursor.tailRecM[Id, Cursor] { c =>
      c.previous match {
        case Some(value) => Left(value)
        case None        => Right(c)
      }
    }

    val rootOperator = rootCursor.op.op
    OperatorTrackedEventRoot(
      rootOperator.id,
      rootOperator.entityId,
      rootOperator.name,
      rootOperator.operatorType,
      rootCursor.op.runAt.toEpochMilli,
      rootCursor.op.finishedAt.map(_.toEpochMilli),
      error,
      rootCursor.op.children.map(toEvent)
    )
  }
}
