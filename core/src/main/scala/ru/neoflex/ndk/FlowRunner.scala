package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.engine.process.ProcessPoolFactory
import ru.neoflex.ndk.engine.{ FlowExecutionEngine, NoOpFlowExecutionObserver }

trait FlowRunner extends ConfigReader[EitherError] {
  def executionConfig: EitherError[ExecutionConfig] = readConfig

  val engine: EitherError[FlowExecutionEngine[EitherError]] = executionConfig.map { c =>
    new FlowExecutionEngine[EitherError](
      new NoOpFlowExecutionObserver[EitherError](),
      c,
      ProcessPoolFactory.create(c.processPool)
    )
  }

  def run(op: FlowOp): Unit = engine.flatMap(_.execute(op)).fold(e => throw new RuntimeException(e.toString), x => x)
}

trait FlowRunnerApp extends FlowRunner with App
