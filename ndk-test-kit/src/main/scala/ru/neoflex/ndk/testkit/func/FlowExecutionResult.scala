package ru.neoflex.ndk.testkit.func

import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Broadcast, GraphDSL, Flow => AkkaFlow, Source => AkkaSource }
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.testkit.func.metric.{ MetricStage, MetricStore }

import scala.concurrent.Future
import scala.concurrent.duration.{ span, DurationInt }

final case class FlowExecutionResult[A <: FlowOp, B](
  private[func] val source: AkkaSource[A, _],
  private[func] val flow: AkkaFlow[RunFlowResult[A], B, _],
  private[func] val flowTraceSink: FlowTraceSink = NoOpFlowTraceSink)

final case class WithMetricsResult[A <: FlowOp, B](
  private[func] val result: FlowExecutionResult[A, B],
  private[func] val metricStore: MetricStore[B])

object FlowExecutionResult {
  implicit class FlowExecutionResultOps[A <: FlowOp, B](r: FlowExecutionResult[A, B]) {
    def filter(f: B => Boolean): FlowExecutionResult[A, B] = FlowExecutionResult(r.source, r.flow.filter(f))
    def map[C](f: B => C): FlowExecutionResult[A, C]       = FlowExecutionResult(r.source, r.flow.map(f))

    def withFlowTraceSink(s: Sink[RunFlowTraceEvent]): FlowExecutionResult[A, B] =
      FlowExecutionResult(r.source, r.flow, StreamedFlowTraceSink(s.s))

    def withMetrics(f: MetricStore[B] => MetricStore[B]): WithMetricsResult[A, B] = {
      WithMetricsResult(r, f(MetricStore[B]()))
    }

    def runWithSink(sink: Sink[B])(implicit actorSystem: ActorSystem, runIdGenerator: RunIdGenerator): RunResult =
      WithMetricsResult(r, MetricStore[B]()).runWithSink(sink)
  }
}

object WithMetricsResult {
  implicit class MetricsResultOps[A <: FlowOp, B](m: WithMetricsResult[A, B]) extends EngineFlowMaker {
    def withFlowTraceSink(s: Sink[RunFlowTraceEvent]): WithMetricsResult[A, B] =
      m.copy(result = m.result.withFlowTraceSink(s))

    def runWithSink(sink: Sink[B])(implicit actorSystem: ActorSystem, runIdGenerator: RunIdGenerator): RunResult = {
      val runId      = runIdGenerator.next
      val resultFlow = m.result
      val alsoToMetric = GraphDSL.createGraph(new MetricStage[B](runId, m.metricStore)) { implicit b => r =>
        val bcast = b.add(Broadcast[B](2, eagerCancel = true))
        bcast.out(1) ~> r.in
        r ~> m.metricStore.sink.s
        FlowShape(bcast.in, bcast.out(0))
      }

      val (processFlow, killSwitch) = makeThroughEngineFlow[A](runId, resultFlow.flowTraceSink)
      val futureResult = resultFlow.source
        .via(processFlow)
        .via(resultFlow.flow)
        .via(alsoToMetric)
        .runWith(sink.s)

      val finalResult = killSwitch.map { ks =>
        futureResult.map { _ =>
          ks.shutdown()
        }(actorSystem.dispatcher)
      }.getOrElse(futureResult)

      RunResult(finalResult)
    }
  }
}

final case class RunResult(f: Future[_]) {
  def awaitResult(timeout: scala.concurrent.duration.Duration = 5 minutes span): Unit = {
    scala.concurrent.Await.result(f, timeout)
  }
}
