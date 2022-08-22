package ru.neoflex.ndk

import cats.arrow.FunctionK
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.engine.process.ProcessPoolFactory
import ru.neoflex.ndk.engine.{ FlowExecutionEngine, NoOpFlowExecutionObserver }

trait FlowRunnerBase {
  def run(op: FlowOp): Unit
}

trait FlowRunner extends ConfigReader[EitherError] with FlowRunnerBase {
  def executionConfig: EitherError[ExecutionConfig] = readConfig

  val engine: EitherError[FlowExecutionEngine[EitherError, EitherError]] = executionConfig.map { c =>
    new FlowExecutionEngine[EitherError, EitherError](
      new NoOpFlowExecutionObserver[EitherError](),
      c,
      ProcessPoolFactory.create(c.processPool),
      FunctionK.id
    )
  }

  override def run(op: FlowOp): Unit = engine.flatMap(_.execute(op)).fold(e => throw e.toThrowable, x => x)
}

trait FlowRunnerApp extends FlowRunner with App
