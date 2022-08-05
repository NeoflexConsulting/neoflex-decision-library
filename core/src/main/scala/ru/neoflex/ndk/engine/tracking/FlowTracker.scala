package ru.neoflex.ndk.engine.tracking

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.syntax.NoId
import ru.neoflex.ndk.engine.ExecutingOperator
import ru.neoflex.ndk.engine.tracking.TrackingOperatorCursor.toEvent
import ru.neoflex.ndk.error.NdkError

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

final class FlowTracker(clock: Clock = JavaDefaultClock) {
  private val trackingFlows = new ConcurrentHashMap[String, TrackingOperatorCursor]().asScala

  private def update(operatorCursor: (String, TrackingOperatorCursor)): Unit = trackingFlows += operatorCursor

  def started[O <: FlowOp](executingOperator: ExecutingOperator[O]): O = {
    import executingOperator.root
    def updatedCursor =
      trackingFlows
        .get(root.id)
        .map { operatorCursor =>
          val nextCursor = Cursor(operatorCursor.cursor.some, TrackingOperator(executingOperator.op, clock.now()))
          operatorCursor.copy(cursor = nextCursor)
        }
        .getOrElse {
          TrackingOperatorCursor(root, Cursor(None, TrackingOperator(root, clock.now())))
        }

    if (root.id != NoId && root.id.nonEmpty) {
      update(root.id -> updatedCursor)
    }

    executingOperator.op
  }

  def finished[O <: FlowOp](executingOperator: ExecutingOperator[O]): Unit = {
    import executingOperator.root
    def updatedOperatorCursor = {
      val operatorCursor = trackingFlows(root.id)
      val currentCursor  = operatorCursor.cursor
      currentCursor.previous.map { prevCursor =>
        val updatedOperator = currentCursor.op.copy(finishedAt = clock.now().some)
        val newCurrentCursor =
          Cursor(prevCursor.previous, prevCursor.op.copy(children = prevCursor.op.children :+ updatedOperator))
        operatorCursor.copy(cursor = newCurrentCursor)
      }.getOrElse {
        operatorCursor.copy(cursor = currentCursor.copy(op = currentCursor.op.copy(finishedAt = clock.now().some)))
      }
    }

    if (root.id != NoId && root.id.nonEmpty) {
      update(root.id -> updatedOperatorCursor)
    }
  }

  def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): O = executingOperator.op

  def executionFinished[O <: FlowOp](
    executingOperator: ExecutingOperator[O],
    error: Option[NdkError]
  ): Option[OperatorTrackedEventRoot] = {
    import executingOperator.root
    Option(root.id)
      .filter(id => id != NoId && id.nonEmpty)
      .flatMap(trackingFlows.get)
      .map { operatorCursor =>
        trackingFlows -= root.id
        toEvent(operatorCursor, error)
      }
  }
}
