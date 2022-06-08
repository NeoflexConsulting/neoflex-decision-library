package ru.neoflex.ndk

import cats.MonadError
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import ru.neoflex.ndk.error.{ ConfigLoadError, NdkError }
import cats.syntax.either._
import pureconfig.generic.auto._

trait ConfigReader[F[_]] {
  def readConfig(implicit monadError: MonadError[F, NdkError]): F[ExecutionConfig] = {
    ConfigSource
      .fromConfig(ConfigFactory.load())
      .at("application.execution")
      .load[ExecutionConfig]
      .leftMap(ConfigLoadError)
      .liftTo[F]
  }
}
