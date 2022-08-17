package ru.neoflex.ndk.testkit.func

import com.aventrix.jnanoid.jnanoid.NanoIdUtils

trait RunIdGenerator {
  def next: String
}

trait RandomRunIdGenerator extends RunIdGenerator {
  private val gen = {
    val alphabet = "_0123456789abcdefghijklmnopqrstuvwxyz".toCharArray
    () => NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, alphabet, 10)
  }

  override def next: String = gen()
}

trait RunIdGeneratorInstances {
  implicit val defaultGenerator: RunIdGenerator = new RandomRunIdGenerator {}
}
