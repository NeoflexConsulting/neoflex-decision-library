package ru.neoflex.ndk.testkit.func

import cats.syntax.either._
import cats.syntax.option._
import akka.stream._
import akka.stream.scaladsl.{ Keep, Flow => StreamFlow, Source => StreamSource }
import cats.arrow.FunctionK
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.dsl.syntax.EitherError
import ru.neoflex.ndk.engine.process.ProcessPoolFactory
import ru.neoflex.ndk.engine.tracking.{ FlowTracker, FlowTrackingObserver, OperatorTrackedEventRoot }
import ru.neoflex.ndk.engine.{ FlowExecutionEngine, NoOpFlowExecutionObserver }
import ru.neoflex.ndk.error.WrappedError
import ru.neoflex.ndk.{ ExecutionConfig, ProcessPoolConfig }

private[func] trait EngineFlowMaker {
  private def makeTrackingObserver(flowTraceSink: FlowTraceSink)(implicit materializer: Materializer) = {
    def offer(q: BoundedSourceQueue[OperatorTrackedEventRoot])(e: OperatorTrackedEventRoot) = {
      q.offer(e) match {
        case QueueOfferResult.Enqueued => Either.right(())
        case r                         => Either.left(WrappedError(new RuntimeException(s"Could not insert event into tracking queue: $r")))
      }
    }

    flowTraceSink match {
      case NoOpFlowTraceSink => new NoOpFlowExecutionObserver[EitherError] -> None
      case StreamedFlowTraceSink(s) =>
        val (queue, killSwitch) = s.runWith {
          StreamSource
            .queue[OperatorTrackedEventRoot](4096)
            .viaMat(KillSwitches.single[OperatorTrackedEventRoot])(Keep.both)
        }

        new FlowTrackingObserver[EitherError](new FlowTracker(), offer(queue)) -> killSwitch.some
    }
  }

  def makeThroughEngineFlow[A <: FlowOp](
    flowTraceSink: FlowTraceSink
  )(implicit materializer: Materializer
  ): (StreamFlow[A, A, _], Option[KillSwitch]) = {
    val (trackingObserver, maybeKillSwitch) = makeTrackingObserver(flowTraceSink)

    val engine = new FlowExecutionEngine[EitherError, EitherError](
      trackingObserver,
      ExecutionConfig.Empty,
      ProcessPoolFactory.create(ProcessPoolConfig()),
      FunctionK.id
    )

    StreamFlow[A].map { f =>
      engine.execute(f).fold(x => throw x.toThrowable, _ => f)
    } -> maybeKillSwitch
  }
}
