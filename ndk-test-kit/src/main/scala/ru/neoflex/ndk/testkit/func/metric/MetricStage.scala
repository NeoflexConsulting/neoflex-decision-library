package ru.neoflex.ndk.testkit.func.metric

import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }

final case class MetricStage[A](runId: String, metricStore: MetricStore[A])
    extends GraphStage[FlowShape[A, RunMetrics]] {

  private val in  = Inlet[A]("MetricStage.in")
  private val out = Outlet[RunMetrics]("MetricStage.out")

  override val shape: FlowShape[A, RunMetrics] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      override def onPush(): Unit = {
        val x = grab(in)
        metricStore.accumulators.foreach {
          case (_, acc) =>
            acc.add(x)
        }
        onPull()
      }

      override def onPull(): Unit = {
        if (!isClosed(in))
          pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        val metrics = metricStore.accumulators.map {
          case (name, acc) =>
            name -> (acc.finish() match {
              case DoubleMetric(value) => value
            })
        }
        push(out, RunMetrics(runId, metrics))
        super.onUpstreamFinish()
      }

      setHandlers(in, out, this)
    }
}
