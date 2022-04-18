package ru.neoflex.ndk.example

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.endine.FlowExecutionEngine
import ru.neoflex.ndk.error.NdkError

object WhileExample extends App {
  case class LoopBodyFlow(in: Application)
      extends Flow(
        "Loop flow",
        flow(
          rule("inc") {
            condition(in.status != "finished") andThen {
              in.score += 1
            }
          },
          gateway("status change") {
            when(in.status == "initial") andThen {
              in.status = "running"
            } and when(in.status == "running") andThen {
              in.status = "finished"
            } otherwise {}
          }
        )
      )

  case class LoopConditionFlow(in: Application)
      extends Flow(
        "Main flow",
        flow(
          whileLoop("status loop", in.status != "finished") {
            LoopBodyFlow(in)
          }
        )
      )

  case class Application(var status: String = "initial", var score: Int = 0)

  val app = Application()
  println(new FlowExecutionEngine[Either[NdkError, *]]().execute(LoopConditionFlow(app)))
  println(app)
}
