package ru.neoflex.ndk

import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.ConfigSource
import ru.neoflex.ndk.error.{ConfigLoadError, NdkError}
import cats.syntax.either._
import pureconfig.generic.auto._

trait ConfigReaderBase {
  val rootConfig: Config = ConfigFactory.load()
}

trait ConfigReader[F[_]] extends ConfigReaderBase {
  def readConfig(implicit monadError: MonadError[F, NdkError]): F[ExecutionConfig] = {
    ConfigSource
      .fromConfig(rootConfig)
      .at("application.execution")
      .load[ExecutionConfig]
      .leftMap(ConfigLoadError)
      .liftTo[F]
  }
}
