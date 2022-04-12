package ru.neoflex.br.dsl

object syntax {
  def rule[T](name: String)(body: => Condition[T]): Rule[T] = {
    Rule(name, body)
  }

  def condition[T](expr: => Boolean): Condition[T] = {
    Condition(() => expr)
  }

  def flow(name: String): SealedFlow = SealedFlow(name, Seq.empty)

  def flow[T](ops: FlowOp*): Seq[FlowOp] = ops

  def gateway[T](name: String)(build: => Gateway[T]): Gateway[T] = build.copy(name = name)

  def when[T](name: String): Gateway.WhenBuilder[T] = Gateway.WhenBuilder(name, Seq.empty)

  def when[T]: Gateway.WhenBuilder[T] = Gateway.WhenBuilder("", Seq.empty)
}
