package ru.neoflex.ndk.engine.tracking

import cats.Monad
import cats.syntax.functor._
import ru.neoflex.ndk.dsl.`type`.OperatorType
import ru.neoflex.ndk.error.NdkError

final case class OperatorTrackedEventRoot(
  id: String,
  name: Option[String],
  `type`: OperatorType,
  error: Option[ExecutionError],
  ops: List[OperatorTrackedEvent])

final case class OperatorTrackedEvent(
  id: String,
  name: Option[String],
  `type`: OperatorType,
  ops: List[OperatorTrackedEvent])

final case class ExecutionError(`type`: String, message: String)
object ExecutionError {
  implicit def toExecutionError(ndkError: NdkError): ExecutionError =
    ExecutionError(ndkError.getClass.getSimpleName, ndkError.message)

  implicit def toExecutionError[F[_]: Monad](ndkError: F[NdkError]): F[ExecutionError] = ndkError.map(toExecutionError)
}
