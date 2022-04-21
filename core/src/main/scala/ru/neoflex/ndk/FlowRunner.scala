package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.engine.{ FlowExecutionEngine, NoOpFlowExecutionObserver }

trait FlowRunner {
  val engine = new FlowExecutionEngine[EitherError](new NoOpFlowExecutionObserver[EitherError]())

  def run(op: FlowOp): Unit = engine.execute(op).fold(e => throw new RuntimeException(e.toString), x => x)
}

trait FlowRunnerApp extends FlowRunner with App
