package ru.neoflex.ndk.testkit

import org.scalactic.source
import org.scalatest.Assertions
import ru.neoflex.ndk.ExecutionConfig
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.engine.FlowExecutionEngine
import ru.neoflex.ndk.engine.process.ProcessPool
import ru.neoflex.ndk.error.NdkError

trait FlowTestingRunner {
  def run(op: FlowOp)(implicit pos: source.Position): NdkExecutionContext = {
    def errorToException(e: NdkError) = Assertions.fail(s"Failed to run operator: $op", e.toThrowable)
    val tracker                       = new FlowExecutionTracker[EitherError]()
    val engine                        = new FlowExecutionEngine[EitherError](tracker, ExecutionConfig.Empty, new ProcessPool())
    engine.execute(op).fold(errorToException, _ => NdkExecutionContext(tracker))
  }
}

final case class NdkExecutionContext(private[testkit] val tracker: FlowExecutionTracker[EitherError])
