package ru.neoflex.ndk.integration.kafka

import akka.kafka.{ CommitterSettings, ConsumerSettings, ProducerSettings }
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer }
import ru.neoflex.ndk.ConfigReaderBase

trait KafkaAppConfigReader extends ConfigReaderBase {

  def readRestAppConfig: KafkaAppConfig = {
    val kafkaConfig     = rootConfig.getConfig("application.kafka")
    val executionConfig = kafkaConfig.getConfig("flow-execution")
    val parallelism     = executionConfig.getInt("parallelism")
    val threadPoolSize  = executionConfig.getInt("thread-pool-size")
    val consumerSettings =
      ConsumerSettings(kafkaConfig.getConfig("consumer-settings"), new ByteArrayDeserializer, new ByteArrayDeserializer)
    val producerSettings =
      ProducerSettings(kafkaConfig.getConfig("producer-settings"), new ByteArraySerializer, new ByteArraySerializer)
    val committerSettings = CommitterSettings(kafkaConfig.getConfig("committer-settings"))
    KafkaAppConfig(parallelism, threadPoolSize, consumerSettings, producerSettings, committerSettings)
  }
}

final case class KafkaAppConfig(
  flowExecutionParallelism: Int,
  flowExecThreadPoolSize: Int,
  consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]],
  producerSettings: ProducerSettings[Array[Byte], Array[Byte]],
  committerSettings: CommitterSettings)
