package ru.neoflex.ndk.dsl

trait ImplicitConversions {
  implicit def stringToOption(s: String): Option[String] = Option(s)
  implicit def charToOption(c: Char): Option[Char]       = Option(c)
  implicit def intToOption(i: Int): Option[Int]          = Option(i)
}

object ImplicitConversions extends ImplicitConversions
