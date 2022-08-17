package ru.neoflex.ndk.testkit.func

import akka.stream.scaladsl.{ Sink => AkkaSink }

import scala.concurrent.Future

private[func] sealed trait FlowTraceSink
private[func] case object NoOpFlowTraceSink extends FlowTraceSink
private[func] final case class StreamedFlowTraceSink(s: AkkaSink[RunFlowTraceEvent, Future[_]])
    extends FlowTraceSink
