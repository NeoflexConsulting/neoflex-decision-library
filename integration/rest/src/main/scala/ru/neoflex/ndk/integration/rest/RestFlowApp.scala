package ru.neoflex.ndk.integration.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ru.neoflex.ndk.FlowRunnerBase
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax.EitherError

import scala.util.{Failure, Success}

trait RestFlowApp extends RestAppConfigReader with FlowRunnerBase {
  def routes: SealedPath

  private def startHttpServer(config: RestConfig, routes: Route)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt(config.host, config.port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def appName: String = getClass.getSimpleName.replaceAll("[^a-zA-Z\\d]", "")

  final def main(args: Array[String]): Unit = {
    val config                       = readRestAppConfig[EitherError].fold(e => throw e.toThrowable, c => c)
    implicit val system: ActorSystem = ActorSystem(appName)
    startHttpServer(config, routes.buildRoute(run))
  }
}
