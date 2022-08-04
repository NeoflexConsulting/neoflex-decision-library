package ru.neoflex.ndk.tracker.config

import cats.MonadError
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.config.ConfigFactory
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}
import ru.neoflex.ndk.ExecutionConfig
import ru.neoflex.ndk.error.{ConfigLoadError, NdkError}

import scala.concurrent.duration.Duration

trait TrackingConfigReader[F[_]] {
  def readAllConfigs(implicit monadError: MonadError[F, NdkError]): F[AllConfigs] = {
    val source = ConfigSource.fromConfig(ConfigFactory.load())
    def loadAt[A: ConfigReader](namespace: String): F[A] =
      source.at(namespace).load[A].leftMap(ConfigLoadError).liftTo[F]

    for {
      executionConfig <- loadAt("application.execution")(exportReader[ExecutionConfig].instance)
      trackingConfig  <- loadAt("application.tracking")(exportReader[TrackingConfig].instance)
      kafkaConfig     <- loadAt("application.kafka")(exportReader[KafkaConfig].instance)
    } yield AllConfigs(executionConfig, trackingConfig, kafkaConfig)
  }
}

final case class TrackingConfig(eventsTopic: String, sendType: TrackingEventSendType, receiveResponseTimeout: Duration)
final case class KafkaConfig(bootstrapServers: String, producerConfigs: Map[String, String] = Map.empty)
final case class AllConfigs(executionConfig: ExecutionConfig, trackingConfig: TrackingConfig, kafkaConfig: KafkaConfig)
