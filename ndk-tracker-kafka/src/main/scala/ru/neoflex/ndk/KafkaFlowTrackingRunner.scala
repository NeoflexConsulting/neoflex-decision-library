package ru.neoflex.ndk

import org.apache.kafka.common.serialization.Serializer
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.engine.FlowExecutionEngine
import ru.neoflex.ndk.engine.process.ProcessPoolFactory
import ru.neoflex.ndk.engine.tracking.OperatorTrackedEventRoot
import ru.neoflex.ndk.tools.Futures
import ru.neoflex.ndk.tracker.config.{ AllConfigs, TrackingConfigReader }
import ru.neoflex.ndk.tracker.kafka.Serialization.string
import ru.neoflex.ndk.tracker.kafka.{ KafkaFlowTracker, KafkaProducerFactory, Serialization }

import scala.concurrent.{ ExecutionContext, Future }

trait KafkaFlowTrackingRunner extends TrackingConfigReader[EitherError] with FlowRunnerBase {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val engine: EitherError[FlowExecutionEngine[EitherError, Future]] = readAllConfigs.map {
    case AllConfigs(executionConfig, trackingConfig) =>
      new FlowExecutionEngine[EitherError, Future](
        new KafkaFlowTracker(
          trackingConfig.eventsTopic,
          trackingConfig.sendType,
          KafkaProducerFactory.create(trackingConfig.bootstrapServers, trackingConfig.producerConfigs)
        ),
        executionConfig,
        ProcessPoolFactory.create(executionConfig.processPool),
        Futures.liftFuture(trackingConfig.receiveResponseTimeout)
      )
  }

  override def run(op: FlowOp): Unit = engine.flatMap(_.execute(op)).fold(e => throw e.toThrowable, x => x)

  implicit def trackingEventSerializer: Serializer[OperatorTrackedEventRoot] =
    Serialization.operatorTrackedEventRootJson
}

trait KafkaFlowTrackingApp extends KafkaFlowTrackingRunner with App
