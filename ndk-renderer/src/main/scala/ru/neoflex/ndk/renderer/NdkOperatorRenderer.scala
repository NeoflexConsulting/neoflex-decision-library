package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.renderer.uml.PlantUmlEncodersImpl

import scala.reflect.runtime.universe.runtimeMirror

object NdkOperatorRenderer {
  private val EventPrefix = "##ndkr"

  def main(args: Array[String]): Unit = {
    val className          = args(0)
    val limitEncodingDepth = args(1).toBoolean
    val flowClass          = Class.forName(className)
    val mirror             = runtimeMirror(flowClass.getClassLoader)
    val dataGenerator      = new DataGenerator(mirror)
    val operator           = dataGenerator.generateAsFlowOp(flowClass)

    render(EncodingContext(operator, 1, 0, limitEncodingDepth), dataGenerator)
  }

  def render(ctx: EncodingContext[FlowOp], dataGenerator: DataGenerator): Unit = {
    val puml      = new PlantUmlEncodersImpl(dataGenerator).encode(ctx)
    val eventText = s"$EventPrefix[${escapeString(puml)}]"

    println(eventText)
  }

  def escapeString(str: String): String = {
    str
      .replaceAll("[\n]", "|n")
      .replaceAll("[\r]", "|r")
  }
}
