package ru.neoflex.ndk.testkit.func.metric

import ru.neoflex.ndk.testkit.func.metric.ExecutionResult.{ MetricFilters, MetricFiltersOps }

sealed trait MeasurementAccumulator[A] {
  def add(x: A): Unit
  def finish(): MetricVal
}

object MeasurementAccumulator {
  implicit class MeasurementAccumulatorOps[A](first: MeasurementAccumulator[A]) {
    private def toMetric[B: Numeric](x: B): MetricVal = DoubleMetric(implicitly[Numeric[B]].toDouble(x))

    def /[B: Numeric](second: B): MeasurementAccumulator[A] =
      CompositeAccumulator(
        first,
        BasicAccumulator(List.empty, (_, m) => m, toMetric(second)),
        (m1, m2) => m1 / m2
      )

    def /(second: MeasurementAccumulator[A]): MeasurementAccumulator[A] = CompositeAccumulator(
      first,
      second,
      (m1, m2) => m1 / m2
    )

    def *[B: Numeric](second: B): MeasurementAccumulator[A] = CompositeAccumulator(
      first,
      BasicAccumulator(List.empty, (_, m) => m, toMetric(second)),
      (m1, m2) => m1 * m2
    )

    def *(second: MeasurementAccumulator[A]): MeasurementAccumulator[A] =
      CompositeAccumulator(first, second, (m1, m2) => m1 * m2)

    def +[B: Numeric](second: B): MeasurementAccumulator[A] = CompositeAccumulator(
      first,
      BasicAccumulator(List.empty, (_, m) => m, toMetric(second)),
      (m1, m2) => m1 + m2
    )

    def +(second: MeasurementAccumulator[A]): MeasurementAccumulator[A] =
      CompositeAccumulator(first, second, (m1, m2) => m1 + m2)

    def -[B: Numeric](second: B): MeasurementAccumulator[A] = CompositeAccumulator(
      first,
      BasicAccumulator(List.empty, (_, m) => m, toMetric(second)),
      (m1, m2) => m1 - m2
    )

    def -(second: MeasurementAccumulator[A]): MeasurementAccumulator[A] =
      CompositeAccumulator(first, second, (m1, m2) => m1 - m2)
  }

}

final case class BasicAccumulator[A](filters: MetricFilters[A], update: (A, MetricVal) => MetricVal, initial: MetricVal)
    extends MeasurementAccumulator[A] {
  private var metric = initial

  override def add(x: A): Unit = {
    filters.accept(x) {
      metric = update(x, metric)
    }
  }

  override def finish(): MetricVal = metric
}
final case class CompositeAccumulator[A](
  first: MeasurementAccumulator[A],
  second: MeasurementAccumulator[A],
  combine: (MetricVal, MetricVal) => MetricVal)
    extends MeasurementAccumulator[A] {

  override def add(x: A): Unit = {
    first.add(x)
    second.add(x)
  }

  override def finish(): MetricVal = {
    combine(first.finish(), second.finish())
  }
}
