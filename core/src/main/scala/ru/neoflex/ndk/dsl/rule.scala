package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.Rule._
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
import ru.neoflex.ndk.dsl.syntax.NoId

final case class Rule(
  override val id: String,
  override val name: Option[String],
  conditions: Seq[Condition],
  otherwise: Option[Otherwise])
    extends RuleOp {

  override def isEmbedded: Boolean = true
}

trait RuleSyntax {
  def rule(body: => RuleBuilder): Rule = rule()(body)
  def rule(id: String = NoId, name: Option[String] = None)(body: => RuleBuilder): Rule = {
    val rb = body
    Rule(id, name, rb.conditions, rb.otherwiseBranch)
  }

  def condition(expr: => Boolean): ConditionBuilder = condition(None, expr)

  def condition(name: Option[String], expr: => Boolean): ConditionBuilder =
    ConditionBuilder(name, () => expr)
}

object Rule {
  final case class ConditionBuilder(
    name: Option[String],
    expr: () => Boolean,
    ruleBuilder: RuleBuilder = RuleBuilder()) {

    def andThen(body: => Unit): RuleBuilder =
      ruleBuilder.copy(conditions = ruleBuilder.conditions :+ Condition(name, expr, () => body))
  }

  final case class RuleBuilder(conditions: Seq[Condition] = Seq.empty, otherwiseBranch: Option[Otherwise] = None) {
    def condition(expr: => Boolean): ConditionBuilder                       = condition(None, expr)
    def condition(name: Option[String], expr: => Boolean): ConditionBuilder = ConditionBuilder(name, () => expr, this)

    def otherwise(body: => Unit): RuleBuilder = otherwise(None, body)

    def otherwise(name: Option[String], body: => Unit): RuleBuilder =
      copy(otherwiseBranch = Some(Otherwise(name, () => body)))
  }

  final case class Condition(name: Option[String], expr: () => Boolean, body: () => Unit) extends DeclarationLocationSupport
  final case class Otherwise(name: Option[String], body: () => Unit) extends DeclarationLocationSupport
}
