package ru.neoflex.ndk.model.py

import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.flowOps

final case class WineModelFlow(data: WineModelData)
    extends Flow(
      "wm-f-1",
      "WineModel",
      flowOps(
        pythonCall[Double, Double](
          "pm-c-1",
          "Call wine model",
          "examples/py-model/src/main/resources/model.py",
          data.features
        ) { result =>
          data.result = result.head
        }
      )
    )

final case class WineModelData(features: List[Double], var result: Double = 0)
