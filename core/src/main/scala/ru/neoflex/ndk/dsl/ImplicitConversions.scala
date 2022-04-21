package ru.neoflex.ndk.dsl

trait ImplicitConversions {
  implicit def stringToOption(s: String): Option[String] = Option(s)
}

object ImplicitConversions extends ImplicitConversions
