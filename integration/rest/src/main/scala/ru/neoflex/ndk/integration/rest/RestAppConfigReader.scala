package ru.neoflex.ndk.integration.rest

import cats.MonadError
import cats.syntax.either._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import ru.neoflex.ndk.ConfigReaderBase
import ru.neoflex.ndk.error.{ ConfigLoadError, NdkError }

trait RestAppConfigReader extends ConfigReaderBase {
  def readRestAppConfig[F[_]](implicit monadError: MonadError[F, NdkError]): F[RestConfig] = {
    val source = ConfigSource.fromConfig(rootConfig)
    source.at("application.rest").load[RestConfig].leftMap(ConfigLoadError).liftTo[F]
  }
}

final case class RestConfig(host: String = "localhost", port: Int = 8080)
