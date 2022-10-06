package ru.neoflex.ndk.strategy.tracking

import cats.arrow.FunctionK
import ru.neoflex.ndk.FlowRunner
import ru.neoflex.ndk.dsl.syntax
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.engine.FlowExecutionEngine
import ru.neoflex.ndk.engine.process.ProcessPoolFactory
import ru.neoflex.ndk.engine.tracking.FlowTracker

trait JsonFileTrackingRunner extends FlowRunner {
  override val engine: syntax.EitherError[FlowExecutionEngine[syntax.EitherError, syntax.EitherError]] =
    executionConfig.map { c =>
      new FlowExecutionEngine[EitherError, EitherError](
        JsonFileFlowTracker(new FlowTracker(), "flow_trace.json"),
        c,
        ProcessPoolFactory.create(c.processPool),
        FunctionK.id
      )
    }
}
