package ru.neoflex.ndk.endine

import ru.neoflex.ndk.dsl.Flow

trait FlowExecutor[F[_]] {
  def execute(flow: Flow): F[Unit]
}
