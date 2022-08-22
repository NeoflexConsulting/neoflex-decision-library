package ru.neoflex.ndk.integration.rest

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import io.circe.{ Decoder, Encoder }
import ru.neoflex.ndk.dsl.FlowOp

trait RoutesDsl {

  implicit def decoderToEntityUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    import io.circe.parser.decode
    Unmarshaller.stringUnmarshaller.map[A] { input =>
      decode(input).fold(throw _, x => x)
    }
  }

  implicit def encoderToEntityMarshaller[A: Encoder]: ToEntityMarshaller[A] = {
    import io.circe.Printer
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { a =>
      Encoder[A].apply(a).printWith(Printer.spaces2)
    }
  }

  def path[A: Decoder, O <: FlowOp, B: Encoder](p: String, toFlow: A => O, toResponse: O => B): SealedPath = {
    import akka.http.scaladsl.server.Directives._
    def buildRoute(execute: FlowOp => Unit): Route = {
      rawPathPrefix(separateOnSlashes(p)) {
        post {
          entity(as[A]) { (a: A) =>
            val flow = toFlow(a)
            execute(flow)
            val response = toResponse(flow)
            complete(response)
          }
        }
      }
    }

    SealedPath(buildRoute)
  }
}

object dsl extends RoutesDsl
