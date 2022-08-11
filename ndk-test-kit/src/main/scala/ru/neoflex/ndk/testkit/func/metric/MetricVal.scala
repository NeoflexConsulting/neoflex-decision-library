package ru.neoflex.ndk.testkit.func.metric

import scala.math.Numeric.DoubleIsFractional

sealed trait MetricVal {
  def /(other: MetricVal): MetricVal
  def *(other: MetricVal): MetricVal
  def +(other: MetricVal): MetricVal
  def -(other: MetricVal): MetricVal
  def /[B: Numeric](other: B): MetricVal
  def *[B: Numeric](other: B): MetricVal
  def +[B: Numeric](other: B): MetricVal
  def -[B: Numeric](other: B): MetricVal
}

final case class DoubleMetric(value: Double) extends MetricVal {
  private def apply(other: MetricVal)(op: (Double, Double) => Double): DoubleMetric = other match {
    case DoubleMetric(v2) => DoubleMetric(op(value, v2))
  }

  override def /(other: MetricVal): MetricVal = apply(other)(DoubleIsFractional.div)
  override def *(other: MetricVal): MetricVal = apply(other)(DoubleIsFractional.times)
  override def +(other: MetricVal): MetricVal = apply(other)(DoubleIsFractional.plus)
  override def -(other: MetricVal): MetricVal = apply(other)(DoubleIsFractional.minus)

  override def /[B: Numeric](other: B): MetricVal = DoubleMetric(value / implicitly[Numeric[B]].toDouble(other))
  override def *[B: Numeric](other: B): MetricVal = DoubleMetric(value * implicitly[Numeric[B]].toDouble(other))
  override def +[B: Numeric](other: B): MetricVal = DoubleMetric(value + implicitly[Numeric[B]].toDouble(other))
  override def -[B: Numeric](other: B): MetricVal = DoubleMetric(value - implicitly[Numeric[B]].toDouble(other))
}
