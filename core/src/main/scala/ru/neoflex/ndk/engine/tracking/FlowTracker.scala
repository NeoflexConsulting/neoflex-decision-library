package ru.neoflex.ndk.engine.tracking

import cats.Id
import cats.implicits.{ catsSyntaxFlatMapIdOps, catsSyntaxOptionId }
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.syntax.NoId
import ru.neoflex.ndk.engine.ExecutingOperator
import ru.neoflex.ndk.engine.tracking.TrackingOperatorCursor.toEvent
import ru.neoflex.ndk.error.NdkError

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

final case class Cursor(previous: Option[Cursor], op: TrackingOperator)

final case class TrackingOperator(op: FlowOp, children: List[TrackingOperator] = List.empty)
final case class TrackingOperatorCursor(rootOperator: FlowOp, cursor: Cursor)
object TrackingOperatorCursor {
  implicit def toEvent(tracker: TrackingOperatorCursor, error: Option[NdkError]): OperatorTrackedEventRoot = {
    def toEvent(to: TrackingOperator): OperatorTrackedEvent = {
      val operator = to.op
      val children = to.children.map { to =>
        toEvent(to)
      }
      OperatorTrackedEvent(operator.id, operator.name, operator.operatorType, children)
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
      rootOperator.name,
      rootOperator.operatorType,
      error,
      rootCursor.op.children.map(toEvent)
    )
  }
}

final class FlowTracker {
  private val trackingFlows = new ConcurrentHashMap[String, TrackingOperatorCursor]().asScala

  private def update(operatorCursor: (String, TrackingOperatorCursor)): Unit = trackingFlows += operatorCursor

  private def started[O <: FlowOp](executingOperator: ExecutingOperator[O]): O = {
    import executingOperator.root
    def updatedCursor =
      trackingFlows
        .get(root.id)
        .map { operatorCursor =>
          val nextCursor = Cursor(operatorCursor.cursor.some, TrackingOperator(executingOperator.op))
          operatorCursor.copy(cursor = nextCursor)
        }
        .getOrElse {
          TrackingOperatorCursor(root, Cursor(None, TrackingOperator(root)))
        }

    if (root.id != NoId && root.id.nonEmpty) {
      update(root.id -> updatedCursor)
    }

    executingOperator.op
  }

  private def finished[O <: FlowOp](executingOperator: ExecutingOperator[O]): Unit = {
    import executingOperator.root
    def updatedOperatorCursor = {
      val operatorCursor = trackingFlows(root.id)
      operatorCursor.cursor.previous.map { c =>
        val newCurrentCursor = Cursor(c.previous, c.op.copy(children = c.op.children :+ operatorCursor.cursor.op))
        operatorCursor.copy(cursor = newCurrentCursor)
      }
    }

    if (root.id != NoId && root.id.nonEmpty) {
      updatedOperatorCursor.foreach { c =>
        update(root.id -> c)
      }
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

  def flowStarted(flow: ExecutingOperator[Flow]): Flow                                                 = started(flow)
  def flowFinished(flow: ExecutingOperator[Flow]): Unit                                                = finished(flow)
  def actionStarted(action: ExecutingOperator[Action]): Action                                         = started(action)
  def actionFinished(action: ExecutingOperator[Action]): Unit                                          = finished(action)
  def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): GatewayOp                                 = started(gateway)
  def gatewayFinished(gateway: ExecutingOperator[GatewayOp]): Unit                                     = finished(gateway)
  def whileStarted(loop: ExecutingOperator[WhileOp]): WhileOp                                          = started(loop)
  def whileFinished(loop: ExecutingOperator[WhileOp]): Unit                                            = finished(loop)
  def forEachStarted(forEach: ExecutingOperator[ForEachOp]): ForEachOp                                 = started(forEach)
  def forEachFinished(forEach: ExecutingOperator[ForEachOp]): Unit                                     = finished(forEach)
  def ruleStarted(rule: ExecutingOperator[RuleOp]): RuleOp                                             = started(rule)
  def ruleFinished(rule: ExecutingOperator[RuleOp]): Unit                                              = finished(rule)
  def tableStarted(table: ExecutingOperator[TableOp]): TableOp                                         = started(table)
  def tableFinished(table: ExecutingOperator[TableOp]): Unit                                           = finished(table)
  def pyOperatorStarted(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): PythonOperatorOp[Any, Any] = started(op)
  def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): Unit                      = finished(op)
  def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): RestService[Any, Any]          = started(op)
  def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): Unit                          = finished(op)
}
