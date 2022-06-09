package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl.ForEachOp
import ru.neoflex.ndk.renderer.{ Encoder, Encoders, EncodersHolder, EncodingContext }

trait ForEachOpDataGeneratingEncoder extends Encoder[ForEachOp] with EncodersHolder with LoopUmlBuilder {
  val dataGenerator: Class[_] => Any

  override def apply(ctx: EncodingContext[ForEachOp]): String = {
    val forEach = ctx.op
    val loopBody = forEach.elementClass.map { cls =>
      val e            = dataGenerator(cls)
      val loopOperator = forEach.body(e)
      encoders.generic(ctx.withOperatorAndDepth(loopOperator))
    }.getOrElse(":action;")

    val loopName = forEach.name.getOrElse("has more elements?")
    buildLoopUml(loopName, loopBody)
  }
}

final case class ForEachOpDataGeneratingEncoderImpl(dataGenerator: Class[_] => Any, encoders: Encoders)
    extends ForEachOpDataGeneratingEncoder
