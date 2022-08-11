package ru.neoflex.ndk.testkit.func.metric

import ru.neoflex.ndk.testkit.func.metric.ExecutionResult.MetricFilters

final case class ExecutionResult[T](filters: MetricFilters[T] = List.empty) {
  def filter(f: T => Boolean): ExecutionResult[T] = ExecutionResult(filters :+ f)

  def count(): MeasurementAccumulator[T] = BasicAccumulator[T](filters, (_, previous) => previous + 1, DoubleMetric(0))

  def sum()(implicit n: Numeric[T]): MeasurementAccumulator[T] =
    BasicAccumulator[T](filters, (e, previous) => previous + e, DoubleMetric(0))

  def sum[A: Numeric](f: T => A): MeasurementAccumulator[T] =
    BasicAccumulator[T](filters, (e, previous) => previous + f(e), DoubleMetric(0))

  def avg()(implicit n: Numeric[T]): MeasurementAccumulator[T] =
    BasicAccumulator[T](filters, (e, previous) => previous + e, DoubleMetric(0)) / totalElements

  def avg[A: Numeric](f: T => A): MeasurementAccumulator[T] =
    BasicAccumulator[T](filters, (e, previous) => previous + f(e), DoubleMetric(0)) / totalElements

  def totalElements: MeasurementAccumulator[T] =
    BasicAccumulator[T](List.empty, (_, previous) => previous + 1, DoubleMetric(0))
}

object ExecutionResult {
  type MetricFilter[A]  = A => Boolean
  type MetricFilters[A] = List[MetricFilter[A]]

  implicit class MetricFiltersOps[A](f: MetricFilters[A]) {
    def accept(x: A)(body: => Unit): Unit = {
      if (f.forall(_(x))) {
        body
      }
    }
  }
}
