package ru.neoflex.ndk.testkit.func.metric

import ru.neoflex.ndk.testkit.func.Sink
import ru.neoflex.ndk.testkit.func.metric.Metric.MetricValueType

final case class MetricStore[A](
  accumulators: Map[String, MeasurementAccumulator[A]] = Map.empty[String, MeasurementAccumulator[A]],
  sink: Sink[Map[String, MetricValueType]] = Sink.ignore)

object MetricStore {
  trait MetricBuilder[T] {
    def as(f: ExecutionResult[T] => MeasurementAccumulator[T]): MetricStore[T]
  }

  implicit class MetricStoreOps[A](ms: MetricStore[A]) {
    def metric(name: String): MetricBuilder[A] = (f: ExecutionResult[A] => MeasurementAccumulator[A]) => {
      val acc = f(ExecutionResult())
      ms.copy(accumulators = ms.accumulators.updated(name, acc))
    }

    def toSink(s: Sink[Map[String, MetricValueType]]): MetricStore[A] = {
      ms.copy(sink = s)
    }
  }
}
