package ru.neoflex.ndk.testkit.func

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.{ span, DurationInt }

abstract class NdkFuncSpec extends AnyFlatSpec with BeforeAndAfterAll {
  protected implicit val actorSystem: ActorSystem = ActorSystem(getClass.getSimpleName)

  override def afterAll(): Unit = {
    super.afterAll()
    Await.ready(actorSystem.terminate(), 5 minutes span)
  }
}
