package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.renderer.uml.PlantUmlEncodersImpl

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe.runtimeMirror

object NdkOperatorRenderer {
  private val EventPrefix = "##ndkr"

  def main(args: Array[String]): Unit = {
    val className     = args(0)
    val flowClass     = Class.forName(className)
    val mirror        = runtimeMirror(flowClass.getClassLoader)
    val dataGenerator = new DataGenerator(mirror)
    val operator      = dataGenerator.generateAsFlowOp(flowClass)

    render(operator, dataGenerator)
  }

  def render(f: FlowOp, dataGenerator: DataGenerator): Unit = {
    val puml      = new PlantUmlEncodersImpl(dataGenerator).encode(f)
    val eventText = s"$EventPrefix[${escapeString(puml)}]"

    println(eventText)
  }

  def escapeString(str: String): String = {
    str
      .replaceAll("[\n]", "|n")
      .replaceAll("[\r]", "|r")
  }
}

object T extends App {
  import scala.reflect.runtime.universe._

  test[Char]()

  def test[A: TypeTag: ClassTag](): Unit = {
    val tt = implicitly[TypeTag[A]]
    val cls = classTag[A].runtimeClass
    val mirror = runtimeMirror(cls.getClassLoader)
    val smb = mirror.classSymbol(cls)
    println(tt)
    println(smb)
    println(tt.tpe)
    println(smb.info)
    println(tt.tpe =:= typeOf[A])
    println(smb.info =:= typeOf[A])
  }
}
