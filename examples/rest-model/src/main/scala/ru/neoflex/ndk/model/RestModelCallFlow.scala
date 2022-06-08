package ru.neoflex.ndk.model

import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.syntax._

case class RestModelCallFlow(data: FlowData)
    extends Flow(
      "rmc-f-1",
      "RestModelCall",
      flowOps(
        serviceCall[MlFlowModelData[Double], List[Double]](
          "sc-1",
          "Call elasticnet wine model",
          serviceEndpoint("http://localhost:5000"),
          "/invocations",
          data.features.mlFlowData
        ) { result =>
          data.result = result
        },

        serviceCall[MlFlowModelData[Double], List[Double]](
          "sc-2",
          "Call wine model service by name",
          serviceName("wine-model"),
          "/invocations",
          data.features.mlFlowData
        ) { result =>
          println(s"Result of invocation service by name: $result")
        },
      )
    )
