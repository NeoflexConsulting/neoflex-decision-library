package ru.neoflex.ndk.tracker.kafka

import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig }
import org.apache.kafka.common.serialization.Serializer

import scala.jdk.CollectionConverters.MapHasAsJava

object KafkaProducerFactory {
  def create[K: Serializer, V: Serializer](
    bootstrapServers: String,
    producerConfigs: Map[String, String] = Map.empty
  ): KafkaProducer[K, V] =
    create[K, V](producerConfigs + (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers))

  def create[K: Serializer, V: Serializer](configs: Map[String, AnyRef]): KafkaProducer[K, V] = {
    new KafkaProducer[K, V](configs.asJava, implicitly[Serializer[K]], implicitly[Serializer[V]])
  }
}
