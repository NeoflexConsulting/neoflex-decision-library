package ru.neoflex.ndk.dsl

final case class While(name: String, condition: () => Boolean, body: FlowOp)                 extends WhileOp
final case class ForEach(name: String, collection: () => Iterable[Any], body: Any => FlowOp) extends ForEachOp

trait WhileSyntax {
  def whileLoop(name: String, condition: => Boolean)(body: FlowOp): While = While(name, () => condition, body)
  def whileLoop(condition: => Boolean)(body: FlowOp): While               = While("", () => condition, body)
}

trait ForEachSyntax {
  def forEach[A](collection: => Iterable[A])(body: A => Unit): ForEach = forEach("noname", collection)(body)

  def forEach[A](name: String, collection: => Iterable[A])(body: A => Unit): ForEach =
    ForEach(
      name,
      () => collection,
      x =>
        SealedAction { () =>
          body.asInstanceOf[Any => Unit](x)
        }
    )

  def forEachOp[A](collection: => Iterable[A])(body: A => FlowOp): ForEach = forEachOp("noname", collection)(body)

  def forEachOp[A](name: String, collection: => Iterable[A])(body: A => FlowOp): ForEach = ForEach(
    name,
    () => collection,
    body.asInstanceOf[Any => FlowOp]
  )
}
