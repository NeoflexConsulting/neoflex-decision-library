package ru.neoflex.ndk.renderer.uml

import scala.collection.mutable

trait LoopUmlBuilder {
  def buildLoopUml(name: String, body: String): String = {
    new mutable.StringBuilder()
      .append("while (")
      .append(name)
      .append(") is (yes)\r\n")
      .append(body)
      .append("\r\nendwhile (no)")
      .result()
  }
}
