package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.syntax.NoId

import scala.reflect.{ classTag, ClassTag }

final case class While(
  override val id: String,
  override val name: Option[String],
  condition: () => Boolean,
  body: FlowOp)
    extends WhileOp

final case class ForEach(
  override val id: String,
  override val name: Option[String],
  collection: () => Iterable[Any],
  body: Any => FlowOp,
  elementClass: Option[Class[_]] = None)
    extends ForEachOp

trait WhileSyntax {
  def whileLoop(condition: => Boolean)(body: FlowOp): While             = While(NoId, None, () => condition, body)
  def whileLoop(id: String, condition: => Boolean)(body: FlowOp): While = whileLoop(id, None, condition)(body)
  def whileLoop(id: String, name: Option[String], condition: => Boolean)(body: FlowOp): While =
    While(id, name, () => condition, body)
}

trait ForEachSyntax {
  def forEach[A](collection: => Iterable[A])(body: A => Unit): ForEach             = forEach(NoId, collection)(body)
  def forEach[A](id: String, collection: => Iterable[A])(body: A => Unit): ForEach = forEach(id, None, collection)(body)
  def forEach[A](id: String, name: Option[String], collection: => Iterable[A])(body: A => Unit): ForEach =
    ForEach(
      id,
      name,
      () => collection,
      x =>
        SealedAction { () =>
          body.asInstanceOf[Any => Unit](x)
        }
    )

  def forEachOp[A: ClassTag](collection: => Iterable[A])(body: A => FlowOp): ForEach =
    forEachOp(NoId, None, collection)(body)
  def forEachOp[A: ClassTag](id: String, collection: => Iterable[A])(body: A => FlowOp): ForEach =
    forEachOp(id, None, collection)(body)

  def forEachOp[A: ClassTag](id: String, name: Option[String], collection: => Iterable[A])(body: A => FlowOp): ForEach =
    ForEach(
      id,
      name,
      () => collection,
      body.asInstanceOf[Any => FlowOp],
      Some(classTag[A].runtimeClass)
    )
}
