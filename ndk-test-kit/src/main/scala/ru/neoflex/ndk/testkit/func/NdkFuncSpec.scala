package ru.neoflex.ndk.testkit.func

import akka.actor.ActorSystem
import org.scalatest.flatspec.AnyFlatSpec

abstract class NdkFuncSpec extends AnyFlatSpec {
  protected implicit val actorSystem: ActorSystem = ActorSystem(getClass.getSimpleName)
}
