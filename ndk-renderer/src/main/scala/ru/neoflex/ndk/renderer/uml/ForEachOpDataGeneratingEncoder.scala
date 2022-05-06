package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl.ForEachOp
import ru.neoflex.ndk.renderer.{ Encoder, Encoders, EncodersHolder }

trait ForEachOpDataGeneratingEncoder extends Encoder[ForEachOp] with EncodersHolder {
  val dataGenerator: Class[_] => Any

  override def apply(forEach: ForEachOp): String = {
    val loopBody = forEach.elementClass.map { cls =>
      val e            = dataGenerator(cls)
      val loopOperator = forEach.body(e)
      encoders.generic(loopOperator)
    }.getOrElse(":action;")

    val loopName = forEach.name.getOrElse("has more elements?")
    s"""
       |while ($loopName) is (yes)
       |$loopBody
       |endwhile (no)""".stripMargin
  }
}

final case class ForEachOpDataGeneratingEncoderImpl(dataGenerator: Class[_] => Any, encoders: Encoders)
    extends ForEachOpDataGeneratingEncoder
