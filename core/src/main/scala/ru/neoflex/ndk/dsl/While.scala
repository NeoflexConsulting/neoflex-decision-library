package ru.neoflex.ndk.dsl

final case class While(name: String, condition: () => Boolean, body: FlowOp) extends WhileOp
final case class Iterate(name: String, body: FlowOp) extends IterateOp

trait WhileSyntax {
  def whileLoop(name: String, condition: => Boolean)(body: FlowOp): While = While(name, () => condition, body)
  def whileLoop(condition: => Boolean)(body: FlowOp): While               = While("", () => condition, body)
}

trait IterateSyntax {
  def iterate[A](name: String, collection: => Iterable[A])(body: A => Unit): Iterate = {
    Iterate(name, Action { () =>
      for (v <- collection) {
        body(v)
      }
    })
  }
}
