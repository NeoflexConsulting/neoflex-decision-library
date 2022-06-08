package ru.neoflex.ndk

import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.python.syntax._

object ExampleApp extends FlowRunnerApp {
  val f = flow("model-f-1", "ModelCallExample") {
    callModel(
      "pm-c-1",
      "catboost_regressor_example.py",
      Seq(Seq(2, 4, 6, 8), Seq(1, 4, 50, 60)),
      "CatBoostExampleModel"
    ) { results =>
      println(results)
    }
  }

  run(f)
}
