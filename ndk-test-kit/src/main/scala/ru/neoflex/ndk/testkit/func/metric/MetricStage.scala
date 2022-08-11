package ru.neoflex.ndk.testkit.func.metric

import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import ru.neoflex.ndk.testkit.func.metric.Metric.MetricValueType

final case class MetricStage[A](metricStore: MetricStore[A])
    extends GraphStage[FlowShape[A, Map[String, MetricValueType]]] {

  private val in  = Inlet[A]("MetricStage.in")
  private val out = Outlet[Map[String, MetricValueType]]("MetricStage.out")

  override val shape: FlowShape[A, Map[String, MetricValueType]] = FlowShape(in, out)

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
        push(out, metrics)
      }

      setHandlers(in, out, this)
    }
}
