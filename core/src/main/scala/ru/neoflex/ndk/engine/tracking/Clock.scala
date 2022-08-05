package ru.neoflex.ndk.engine.tracking

import java.time.Instant

trait Clock {
  def now(): Instant
}

object JavaDefaultClock extends Clock {
  override def now(): Instant = Instant.now()
}
