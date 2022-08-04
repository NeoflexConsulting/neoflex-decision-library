package ru.neoflex.ndk.tools

import cats.arrow.FunctionK
import cats.implicits.toBifunctorOps
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.error.WrappedError

import scala.concurrent.duration.{ Duration, DurationInt }
import scala.concurrent.{ Await, Future }
import scala.util.Try

object Futures {
  def liftFuture(timeout: Duration = 1.seconds): FunctionK[Future, EitherError] = new FunctionK[Future, EitherError] {
    override def apply[A](fa: Future[A]): EitherError[A] =
      Try(Await.result(fa, timeout)).toEither.leftMap(WrappedError)
  }
}
