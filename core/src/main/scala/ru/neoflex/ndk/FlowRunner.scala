package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.{ Flow, FlowOp }
import ru.neoflex.ndk.engine.FlowExecutionEngine

trait FlowRunner {
  val engine = new FlowExecutionEngine[EitherError]()

  def run(op: FlowOp): Unit = runOperator(op).fold(e => throw new RuntimeException(e.toString), x => x)

  private def runOperator(op: FlowOp): EitherError[Unit] = {
    op match {
      case f: Flow => engine.execute(f)
      case _       => engine.execute(flow("Top level flow")(op))
    }
  }
}

trait FlowRunnerApp extends FlowRunner with App
