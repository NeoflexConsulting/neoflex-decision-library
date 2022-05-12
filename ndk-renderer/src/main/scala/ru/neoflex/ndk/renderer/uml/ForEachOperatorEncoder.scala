package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl.ForEachOp
import ru.neoflex.ndk.renderer.{Encoder, EncodingContext}

trait ForEachOperatorEncoder extends Encoder[ForEachOp] {
  override def apply(ctx: EncodingContext[ForEachOp]): String = {
    val loopName = ctx.op.name.getOrElse("has more elements?")
    s"""
       |while ($loopName) is (yes)
       |:action;
       |endwhile (no)""".stripMargin
  }
}

object ForEachOperatorEncoder extends ForEachOperatorEncoder
