package ru.neoflex.ndk.model.py

import ru.neoflex.ndk.FlowRunnerApp

object WinePyModelExample extends FlowRunnerApp {
  val modelData = WineModelData(List(7.4, 0.7, 0, 1.9, 0.076, 11, 34, 0.9978, 3.51, 0.56, 9.4))
  val flow = WineModelFlow(modelData)

  run(flow)

  println(s"Prediction result: ${modelData.result}")
}
