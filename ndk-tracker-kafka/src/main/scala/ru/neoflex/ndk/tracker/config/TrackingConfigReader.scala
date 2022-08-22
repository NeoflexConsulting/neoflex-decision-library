package ru.neoflex.ndk.tracker.config

import cats.MonadError
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import pureconfig.generic.auto._
import pureconfig.{ ConfigReader, ConfigSource }
import ru.neoflex.ndk.error.{ ConfigLoadError, NdkError }
import ru.neoflex.ndk.{ ConfigReaderBase, ExecutionConfig }

import scala.concurrent.duration.Duration

trait TrackingConfigReader[F[_]] extends ConfigReaderBase {
  def readAllConfigs(implicit monadError: MonadError[F, NdkError]): F[AllConfigs] = {
    val source = ConfigSource.fromConfig(rootConfig)
    def loadAt[A: ConfigReader](namespace: String): F[A] =
      source.at(namespace).load[A].leftMap(ConfigLoadError).liftTo[F]

    for {
      executionConfig <- loadAt("application.execution")(exportReader[ExecutionConfig].instance)
      trackingConfig  <- loadAt("application.tracking")(exportReader[TrackingConfig].instance)
    } yield AllConfigs(executionConfig, trackingConfig)
  }
}

final case class TrackingConfig(
  bootstrapServers: String,
  eventsTopic: String,
  sendType: TrackingEventSendType,
  receiveResponseTimeout: Duration,
  producerConfigs: Map[String, String] = Map.empty)
final case class AllConfigs(executionConfig: ExecutionConfig, trackingConfig: TrackingConfig)
