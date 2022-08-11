package ru.neoflex.ndk.testkit.func

import akka.stream.scaladsl.{ Sink => StreamSink }
import ru.neoflex.ndk.engine.tracking.OperatorTrackedEventRoot

import scala.concurrent.Future

private[func] sealed trait FlowTraceSink
private[func] case object NoOpFlowTraceSink extends FlowTraceSink
private[func] final case class StreamedFlowTraceSink(s: StreamSink[OperatorTrackedEventRoot, Future[_]])
    extends FlowTraceSink
