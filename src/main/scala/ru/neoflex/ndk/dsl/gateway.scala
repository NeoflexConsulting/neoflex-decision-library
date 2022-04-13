package ru.neoflex.ndk.dsl
import ru.neoflex.ndk.dsl.Gateway.When

final case class Gateway(name: String, whens: Seq[When], otherwise: () => Unit) extends GatewayOp

object Gateway {
  final case class SealedWhens(whens: Seq[When]) {
    def and(wb: WhenBuilder): WhenBuilder = {
      wb.whens = whens
      wb
    }
    def otherwise(f: => Unit): Gateway = Gateway("", whens, () => f)
  }
  final case class WhenBuilder(name: String, var whens: Seq[When]) {
    private var exprValue: () => Boolean = _
    private var action: () => Unit       = _

    def apply(expr: => Boolean): WhenBuilder = {
      exprValue = () => expr
      this
    }

    def run(f: => Unit): SealedWhens = {
      action = () => f
      SealedWhens(whens :+ toWhen)
    }

    def toWhen: When = When(name, exprValue, action)
  }

  final case class When(name: String, cond: () => Boolean, run: () => Unit)
}

trait GatewaySyntax {
  def gateway(name: String)(build: => Gateway): Gateway = build.copy(name = name)

  def when(name: String): Gateway.WhenBuilder = Gateway.WhenBuilder(name, Seq.empty)

  def when: Gateway.WhenBuilder = Gateway.WhenBuilder("", Seq.empty)
}
