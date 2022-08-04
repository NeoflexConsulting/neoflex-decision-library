package ru.neoflex.ndk.tracker.kafka

import org.apache.kafka.clients.producer.{ Callback, KafkaProducer, ProducerRecord, RecordMetadata }
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.engine.tracking.{ FlowTracker, OperatorTrackedEvent }
import ru.neoflex.ndk.engine.{ ExecutingOperator, FlowExecutionObserver }
import ru.neoflex.ndk.tools.Logging
import ru.neoflex.ndk.tracker.config.{ FireAndForget, TrackingEventSendType, WaitResponse }

import scala.concurrent.Future.successful
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.Try

class KafkaFlowTracker(
  eventsTopic: String,
  sendType: TrackingEventSendType,
  producer: KafkaProducer[String, OperatorTrackedEvent]
)(implicit ec: ExecutionContext)
    extends FlowExecutionObserver[Future]
    with Logging {
  private val flowTracker = new FlowTracker

  override def executionStarted[O <: FlowOp](executingOperator: ExecutingOperator[O]): Future[O] =
    successful(flowTracker.executionStarted(executingOperator))

  override def executionFinished[O <: FlowOp](executingOperator: ExecutingOperator[O]): Future[Unit] = {
    def logError(e: Throwable): Unit =
      logger.warn(s"Error has occurred while sending tracking event to kafka. Operator: ${executingOperator.id}", e)

    def wrapWithSendType(future: Future[Unit]): Future[Unit] = sendType match {
      case FireAndForget =>
        future.recover(logError(_))
        Future.successful(())
      case WaitResponse(ignoreErrors) if ignoreErrors => future.recover(logError(_))
      case WaitResponse(_)                            => future
    }

    def sendEvent(event: OperatorTrackedEvent): Future[Unit] = {
      val record  = new ProducerRecord[String, OperatorTrackedEvent](eventsTopic, event.id, event)
      val promise = Promise[Unit]()

      def send() = producer.send(
        record,
        new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (exception == null) promise.success(())
            else promise.failure(exception)
          }
        }
      )

      Try(send()).fold(e => { promise.failure(e); () }, _ => ())

      wrapWithSendType(promise.future)
    }

    flowTracker.executionFinished(executingOperator).map(sendEvent).getOrElse(Future.successful(()))
  }

  override def flowStarted(flow: ExecutingOperator[Flow]): Future[Flow] =
    successful(flowTracker.flowStarted(flow))

  override def flowFinished(flow: ExecutingOperator[Flow]): Future[Unit] =
    successful(flowTracker.flowFinished(flow))

  override def actionStarted(action: ExecutingOperator[Action]): Future[Action] =
    successful(flowTracker.actionStarted(action))

  override def actionFinished(action: ExecutingOperator[Action]): Future[Unit] =
    successful(flowTracker.actionFinished(action))

  override def gatewayStarted(gateway: ExecutingOperator[GatewayOp]): Future[GatewayOp] =
    successful(flowTracker.gatewayStarted(gateway))

  override def gatewayFinished(gateway: ExecutingOperator[GatewayOp]): Future[Unit] =
    successful(flowTracker.gatewayFinished(gateway))

  override def whileStarted(loop: ExecutingOperator[WhileOp]): Future[WhileOp] =
    successful(flowTracker.whileStarted(loop))

  override def whileFinished(loop: ExecutingOperator[WhileOp]): Future[Unit] =
    successful(flowTracker.whileFinished(loop))

  override def forEachStarted(forEach: ExecutingOperator[ForEachOp]): Future[ForEachOp] =
    successful(flowTracker.forEachStarted(forEach))

  override def forEachFinished(forEach: ExecutingOperator[ForEachOp]): Future[Unit] =
    successful(flowTracker.forEachFinished(forEach))

  override def ruleStarted(rule: ExecutingOperator[RuleOp]): Future[RuleOp] =
    successful(flowTracker.ruleStarted(rule))

  override def ruleFinished(rule: ExecutingOperator[RuleOp]): Future[Unit] =
    successful(flowTracker.ruleFinished(rule))

  override def tableStarted(table: ExecutingOperator[TableOp]): Future[TableOp] =
    successful(flowTracker.tableStarted(table))

  override def tableFinished(table: ExecutingOperator[TableOp], executedRows: Int): Future[Unit] =
    successful(flowTracker.tableFinished(table))

  override def pyOperatorStarted(
    op: ExecutingOperator[PythonOperatorOp[Any, Any]]
  ): Future[PythonOperatorOp[Any, Any]] =
    successful(flowTracker.pyOperatorStarted(op))

  override def pyOperatorFinished(op: ExecutingOperator[PythonOperatorOp[Any, Any]]): Future[Unit] =
    successful(flowTracker.pyOperatorFinished(op))

  override def restServiceStarted(op: ExecutingOperator[RestService[Any, Any]]): Future[RestService[Any, Any]] =
    successful(flowTracker.restServiceStarted(op))

  override def restServiceFinished(op: ExecutingOperator[RestService[Any, Any]]): Future[Unit] =
    successful(flowTracker.restServiceFinished(op))
}
