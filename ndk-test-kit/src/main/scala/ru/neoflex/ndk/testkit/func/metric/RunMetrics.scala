package ru.neoflex.ndk.testkit.func.metric

import ru.neoflex.ndk.testkit.func.metric.Metric.MetricValueType

final case class RunMetrics(runId: String, metrics: Map[String, MetricValueType])
