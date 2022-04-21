package ru.neoflex.ndk.engine

import ru.neoflex.ndk.dsl.FlowOp

trait FlowExecutor[F[_]] {
  def execute(operator: FlowOp): F[Unit]
}
