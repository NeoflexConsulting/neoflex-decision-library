package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.renderer.uml.PlantUmlEncoders

import scala.reflect.runtime.universe.runtimeMirror

object NdkOperatorRenderer {
  private val EventPrefix = "##ndkr"

  def main(args: Array[String]): Unit = {
    val className = args(0)
    val flowClass = Class.forName(className)
    val mirror = runtimeMirror(flowClass.getClassLoader)
    val operator  = new DataGenerator(mirror).generate(flowClass)

    render(operator)
  }

  def render(f: FlowOp): Unit = {
    val puml      = PlantUmlEncoders.encode(f)
    val eventText = s"$EventPrefix[${escapeString(puml)}]"

    println(eventText)
  }

  def escapeString(str: String): String = {
    str
      .replaceAll("[\n]", "|n")
      .replaceAll("[\r]", "|r")
  }
}
