package ru.neoflex.ndk.dsl
import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.syntax.NoId

final case class Gateway(override val id: String, override val name: Option[String], whens: Seq[When], otherwise: FlowOp) extends GatewayOp

object Gateway {
  final case class SealedWhens(whens: Seq[When]) {
    def and(wb: WhenBuilder): WhenBuilder = {
      wb.whens = whens
      wb
    }

    def otherwise(f: => Unit): Gateway = Gateway(NoId, None, whens, SealedAction(() => f))

    def otherwise(op: FlowOp): Gateway = Gateway(NoId, None, whens, op)
  }
  final case class WhenBuilder(name: String, var whens: Seq[When]) {
    private var exprValue: () => Boolean = _
    private var action: FlowOp       = _

    def apply(expr: => Boolean): WhenBuilder = {
      exprValue = () => expr
      this
    }

    def andThen(f: => Unit): SealedWhens = {
      action = SealedAction(() => f)
      SealedWhens(whens :+ toWhen)
    }

    def andThen(op: FlowOp): SealedWhens = {
      action = op
      SealedWhens(whens :+ toWhen)
    }

    def toWhen: When = When(name, exprValue, action)
  }

  final case class When(name: String, cond: () => Boolean, op: FlowOp)
}

trait GatewaySyntax {
  def gateway(id: String, name: Option[String] = None)(build: => Gateway): Gateway = build.copy(id = id, name = name)

  def when(name: String): Gateway.WhenBuilder = Gateway.WhenBuilder(name, Seq.empty)

  def when: Gateway.WhenBuilder = Gateway.WhenBuilder("", Seq.empty)
}
