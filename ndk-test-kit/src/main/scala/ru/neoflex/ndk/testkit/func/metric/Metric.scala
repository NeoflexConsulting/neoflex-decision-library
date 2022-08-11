package ru.neoflex.ndk.testkit.func.metric

case class Metric[A](name: String, value: A)

object Metric {
  type MetricValueType = Double
}
