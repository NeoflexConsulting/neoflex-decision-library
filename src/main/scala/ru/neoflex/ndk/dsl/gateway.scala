package ru.neoflex.ndk.dsl
import ru.neoflex.ndk.dsl.Gateway.When

final case class Gateway(name: String, whens: Seq[When], otherwise: FlowOp) extends GatewayOp

object Gateway {
  final case class SealedWhens(whens: Seq[When]) {
    def and(wb: WhenBuilder): WhenBuilder = {
      wb.whens = whens
      wb
    }

    def otherwise(f: => Unit): Gateway = Gateway("", whens, Action(() => f))

    def otherwise(op: FlowOp): Gateway = Gateway("", whens, op)
  }
  final case class WhenBuilder(name: String, var whens: Seq[When]) {
    private var exprValue: () => Boolean = _
    private var action: FlowOp       = _

    def apply(expr: => Boolean): WhenBuilder = {
      exprValue = () => expr
      this
    }

    def run(f: => Unit): SealedWhens = {
      action = Action(() => f)
      SealedWhens(whens :+ toWhen)
    }

    def run(op: FlowOp): SealedWhens = {
      action = op
      SealedWhens(whens :+ toWhen)
    }

    def toWhen: When = When(name, exprValue, action)
  }

  final case class When(name: String, cond: () => Boolean, op: FlowOp)
}

trait GatewaySyntax {
  def gateway(name: String)(build: => Gateway): Gateway = build.copy(name = name)

  def when(name: String): Gateway.WhenBuilder = Gateway.WhenBuilder(name, Seq.empty)

  def when: Gateway.WhenBuilder = Gateway.WhenBuilder("", Seq.empty)
}
