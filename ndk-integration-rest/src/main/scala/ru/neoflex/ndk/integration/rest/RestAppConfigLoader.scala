package ru.neoflex.ndk.integration.rest

import cats.MonadError
import cats.syntax.either._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import ru.neoflex.ndk.error.{ ConfigLoadError, NdkError }

trait RestAppConfigLoader {
  def readRestAppConfig[F[_]](implicit monadError: MonadError[F, NdkError]): F[RestConfig] = {
    val source = ConfigSource.fromConfig(ConfigFactory.load())
    source.at("application.rest").load[RestConfig].leftMap(ConfigLoadError).liftTo[F]
  }
}

final case class RestConfig(host: String = "localhost", port: Int = 8080)
