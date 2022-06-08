package ru.neoflex.ndk.model

import ru.neoflex.ndk.FlowRunnerApp

object ExampleApp extends FlowRunnerApp {
  val flowData = FlowData(List(7.4, 0.7, 0, 1.9, 0.076, 11, 34, 0.9978, 3.51, 0.56, 9.4))
  val flow = RestModelCallFlow(flowData)

  run(flow)

  println(flowData.result)
}

final case class FlowData(features: List[Double], var result: List[Double] = List.empty)
