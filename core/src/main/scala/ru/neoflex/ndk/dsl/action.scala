package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.syntax.NoId

abstract class ActionBase(override val id: String, override val f: () => Unit, override val name: Option[String] = None)
    extends Action {
  def this(f: => Unit) = {
    this(NoId, () => f)
  }

  def this(id: String, f: => Unit) = {
    this(id, () => f)
  }

  override def apply(): Unit = f()
}

final case class SealedAction(
  override val f: () => Unit,
  override val id: String = NoId,
  override val name: Option[String] = None)
    extends ActionBase(id, f, name) {

  override def isEmbedded: Boolean = true
}

trait ActionSyntax {
  def action(f: => Unit): SealedAction                                                 = SealedAction(() => f)
  def action(id: String = NoId, name: Option[String] = None)(f: => Unit): SealedAction = SealedAction(() => f, id, name)
}
