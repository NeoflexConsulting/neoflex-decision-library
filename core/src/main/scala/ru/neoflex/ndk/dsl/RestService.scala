package ru.neoflex.ndk.dsl

import cats.MonadError
import cats.implicits.catsSyntaxApplicativeErrorId
import io.circe.{Decoder, Encoder}
import ru.neoflex.ndk.RestConfig
import ru.neoflex.ndk.dsl.MlFlowModelData.MlFlowModelDataOps
import ru.neoflex.ndk.dsl.RestService.ServiceNameOrEndpoint
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
import ru.neoflex.ndk.error.{NdkError, RestServiceError}

import scala.util.Try

final case class RestServiceImpl[Req: Encoder, Resp: Decoder](
  override val id: String,
  override val name: Option[String],
  override val serviceNameOrEndpoint: ServiceNameOrEndpoint,
  override val makeUri: () => String,
  override val inputBody: () => Option[Req],
  override val responseCollector: Resp => Unit)
    extends RestService[Req, Resp] with DeclarationLocationSupport {
  override def isEmbedded: Boolean = true
}

trait RestServiceSyntax {
  def serviceName(name: String): ServiceNameOrEndpoint = Left(name)

  def serviceEndpoint(endpoint: String): ServiceNameOrEndpoint = Right(endpoint)

  def serviceCall[Req: Encoder, Resp: Decoder](
    id: String,
    serviceNameOrEndpoint: ServiceNameOrEndpoint,
    uri: => String
  )(
    responseCollector: Resp => Unit
  ): RestService[Req, Resp] = serviceCall(id, None, serviceNameOrEndpoint, uri)(responseCollector)

  def serviceCall[Req: Encoder, Resp: Decoder](
    id: String,
    serviceNameOrEndpoint: ServiceNameOrEndpoint,
    uri: => String,
    body: => Req
  )(
    responseCollector: Resp => Unit
  ): RestService[Req, Resp] = serviceCall(id, None, serviceNameOrEndpoint, uri, body)(responseCollector)

  def serviceCall[Req: Encoder, Resp: Decoder](
    id: String,
    name: Option[String],
    serviceNameOrEndpoint: ServiceNameOrEndpoint,
    uri: => String,
    body: => Req
  )(
    responseCollector: Resp => Unit
  ): RestService[Req, Resp] =
    RestServiceImpl[Req, Resp](id, name, serviceNameOrEndpoint, () => uri, () => Some(body), responseCollector)

  def serviceCall[Req: Encoder, Resp: Decoder](
    id: String,
    name: Option[String],
    serviceNameOrEndpoint: ServiceNameOrEndpoint,
    uri: => String
  )(
    responseCollector: Resp => Unit
  ): RestService[Req, Resp] =
    RestServiceImpl[Req, Resp](id, name, serviceNameOrEndpoint, () => uri, () => None, responseCollector)

  implicit def toMlFlowModelDataOpsOneDimension[T: Encoder](data: List[T]): MlFlowModelDataOps[T] =
    new MlFlowModelDataOps[T](List(data))

  implicit def toMlFlowModelDataOps[T: Encoder](data: List[List[T]]): MlFlowModelDataOps[T] =
    new MlFlowModelDataOps[T](data)
}

final case class MlFlowModelData[T: Encoder](data: List[List[T]])
object MlFlowModelData {
  implicit def mlFlowModelDataEncoder[T: Encoder]: Encoder[MlFlowModelData[T]] =
    io.circe.generic.semiauto.deriveEncoder[MlFlowModelData[T]]

  class MlFlowModelDataOps[T: Encoder](data: List[List[T]]) {
    def mlFlowData: MlFlowModelData[T] = MlFlowModelData[T](data)
  }
}

object RestServiceImplicits {

  implicit class RestServiceOps[F[_]](rs: RestService[Any, Any]) {
    import org.http4s.dsl.io._
    import org.http4s.Uri
    import org.http4s.circe._
    import org.http4s._
    import cats.effect.IO
    import cats.syntax.either._
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    import org.http4s.ember.client.EmberClientBuilder
    import cats.effect.unsafe.implicits.global

    def executeRequest()(implicit monadError: MonadError[F, NdkError], restConfig: RestConfig): F[Unit] = {
      def getEndpoint: F[String] = {
        val endpoint = rs.serviceNameOrEndpoint match {
          case Left(serviceName) =>
            restConfig.endpoints
              .get(serviceName)
              .toRight(
                RestServiceError(
                  new IllegalStateException(s"Could not find endpoint for service: $serviceName"),
                  rs
                )
              )
          case Right(endpoint) => Right(endpoint)
        }
        endpoint.liftTo[F]
      }

      def makeCall() =
        for {
          endpoint  <- getEndpoint
          parsedUri <- Uri.fromString(s"$endpoint/${rs.makeUri()}").leftMap(RestServiceError(_, rs)).liftTo[F]
        } yield {
          val executionResult = EmberClientBuilder.default[IO].build.use { client =>
            val inputJson   = rs.encodedInputBody
            val inputEntity = inputJson.map(jsonEncoder[IO].toEntity).getOrElse(Entity.empty)
            val request     = Request(POST, parsedUri, body = inputEntity.body, headers = Headers("Content-type" -> "application/json"))
            client.run(request).use {
              case Status.Successful(r) =>
                r.attemptAs[String]
                  .map { bodyString =>
                    monadError.fromEither {
                      rs.collectResponse(Option(bodyString).filter(_.nonEmpty)).leftMap(RestServiceError(_, rs))
                    }
                  }
                  .leftMap(RestServiceError(_, rs))
                  .value
                  .map(monadError.fromEither)
                  .map(_.flatten)

              case r =>
                r.as[String].map { e =>
                  RestServiceError(new IllegalStateException(e), rs).raiseError[F, Unit]
                }
            }
          }

          Try(executionResult.unsafeRunSync()).toEither.leftMap(RestServiceError(_, rs)).liftTo[F]
        }

      makeCall().flatMap(_.flatten)
    }
  }
}
