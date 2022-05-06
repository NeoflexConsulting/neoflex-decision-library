package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl.ForEachOp
import ru.neoflex.ndk.renderer.Encoder

trait ForEachOperatorEncoder extends Encoder[ForEachOp] {
  override def apply(forEach: ForEachOp): String = {
    val loopName = forEach.name.getOrElse("has more elements?")
    s"""
       |while ($loopName) is (yes)
       |:action;
       |endwhile (no)""".stripMargin
  }
}

object ForEachOperatorEncoder extends ForEachOperatorEncoder
