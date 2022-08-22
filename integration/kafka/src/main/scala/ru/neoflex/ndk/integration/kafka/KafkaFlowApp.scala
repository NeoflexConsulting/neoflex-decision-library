package ru.neoflex.ndk.integration.kafka

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ProducerMessage, Subscriptions}
import akka.stream.RestartSettings
import akka.stream.scaladsl.{RestartSink, RestartSource, RunnableGraph}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import ru.neoflex.ndk.FlowRunnerBase
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.integration.kafka.StreamsConfig._
import ru.neoflex.ndk.tools.Logging

import java.util.concurrent.Executors
import scala.concurrent.duration.{DurationInt, span}
import scala.concurrent.{ExecutionContext, Future}

trait KafkaFlowApp extends KafkaAppConfigReader with Logging with FlowRunnerBase {
  type RecordKey = Array[Byte]

  val kafkaConfig: KafkaAppConfig = readRestAppConfig

  implicit def flowExecutionContext: ExecutionContext =
    scala.concurrent.ExecutionContext
      .fromExecutorService(Executors.newFixedThreadPool(kafkaConfig.flowExecThreadPoolSize))

  def streams: StreamsConfig

  def createStream[IV: Decoder, O <: FlowOp, OV: Encoder](
    inputTopic: String,
    outputTopic: String,
    toFlow: KafkaRecord[RecordKey, IV] => O,
    toResult: O => KafkaRecord[RecordKey, OV]
  ): StreamsConfig = {
    val restartSettings = RestartSettings(5 seconds span, 30 seconds span, 0.2).withMaxRestarts(1, 5 minutes span)
    val source = RestartSource.onFailuresWithBackoff(restartSettings) { () =>
      Consumer.sourceWithOffsetContext(kafkaConfig.consumerSettings, Subscriptions.topics(Set(inputTopic))).asSource
    }
    val sink = RestartSink.withBackoff(restartSettings) { () =>
      Producer.committableSinkWithOffsetContext(kafkaConfig.producerSettings, kafkaConfig.committerSettings)
    }

    source
      .asSourceWithContext(_._2)
      .map(_._1)
      .map { consumerRecord: ConsumerRecord[RecordKey, Array[Byte]] =>
        val value = Decoder[IV].decode(consumerRecord.value()).fold(throw _, x => x)
        KafkaRecord(consumerRecord.key(), value)
      }
      .map(toFlow)
      .mapAsync(kafkaConfig.flowExecutionParallelism) { flow =>
        Future(run(flow)).map(_ => flow)
      }
      .map(toResult)
      .map { kafkaRecord =>
        val valueBytes     = Encoder[OV].encode(kafkaRecord.value)
        val producerRecord = new ProducerRecord[RecordKey, Array[Byte]](outputTopic, kafkaRecord.key, valueBytes)
        ProducerMessage.single(producerRecord)
      }
      .log(s"$inputTopic => $outputTopic")
      .to(sink)
      .toConfig
  }

  def appName: String = getClass.getSimpleName.replaceAll("[^a-zA-Z\\d]", "")

  final def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem(appName)
    streams.graphs.foreach { g =>
      g.run()
    }
  }
}

final case class StreamsConfig(graphs: Seq[RunnableGraph[_]])
object StreamsConfig {
  implicit class RunnableGraphOps(g: RunnableGraph[_]) {
    def toConfig: StreamsConfig = StreamsConfig(Seq(g))
  }

  implicit class StreamConfigOps(c: StreamsConfig) {
    def ~(o: StreamsConfig): StreamsConfig = StreamsConfig(c.graphs ++ o.graphs)
  }
}

final case class KafkaRecord[K, V](key: K, value: V)
object KafkaRecord {
  def apply[V](value: V): KafkaRecord[Array[Byte], V] = KafkaRecord(null, value)
}
