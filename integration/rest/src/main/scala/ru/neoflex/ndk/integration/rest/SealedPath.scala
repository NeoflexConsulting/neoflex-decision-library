package ru.neoflex.ndk.integration.rest

import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.Route
import ru.neoflex.ndk.dsl.FlowOp

final case class SealedPath(buildRoute: (FlowOp => Unit) => Route)

object SealedPath {
  implicit class SealedPathConcat(p: SealedPath) {
    def ~(other: SealedPath): SealedPath = {
      SealedPath { e =>
        p.buildRoute(e) ~ other.buildRoute(e)
      }
    }
  }
}
