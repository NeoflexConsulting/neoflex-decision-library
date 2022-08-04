package ru.neoflex.ndk.testkit

import cats.arrow.FunctionK
import org.scalactic.source
import org.scalatest.Assertions
import ru.neoflex.ndk.ExecutionConfig
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.engine.process.ProcessPool
import ru.neoflex.ndk.engine.{FlowExecutionEngine, FlowExecutionObserverComposite}
import ru.neoflex.ndk.error.NdkError

class PatchedOperatorRunner(operator: FlowOp, operatorsToReplace: Map[String, FlowOp]) {
  def run(after: NdkExecutionContext => Unit)(implicit pos: source.Position): Unit = {
    def errorToException(e: NdkError) = Assertions.fail(s"Failed to run operator: $operator", e.toThrowable)
    val tracker                       = new FlowExecutionTracker[EitherError]()
    val observer = new FlowExecutionObserverComposite[EitherError](
      new FlowPatchingObserver[EitherError](operatorsToReplace),
      tracker
    )
    val engine = new FlowExecutionEngine[EitherError, EitherError](observer, ExecutionConfig.Empty, new ProcessPool(), FunctionK.id)
    val ctx    = engine.execute(operator).fold(errorToException, _ => NdkExecutionContext(tracker))
    after(ctx)
  }
}

trait OperatorPatching {
  implicit class OperatorPatchOps(op: FlowOp) {
    def withOperators(idAndOperator: (String, FlowOp)*): PatchedOperatorRunner =
      new PatchedOperatorRunner(op, idAndOperator.toMap)
  }
}
