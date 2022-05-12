package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.implicits.CallableActionOps
import ru.neoflex.ndk.dsl.syntax.OperatorOps
import ru.neoflex.ndk.renderer.{ DepthLimitedEncoder, Encoder, Encoders, EncodingContext }

import scala.collection.mutable

trait PlantUmlEncoders extends Encoders with Constants with DepthLimitedEncoder {
  override val encoders: Encoders       = this
  override def generic: Encoder[FlowOp] = this

  def encode(op: FlowOp): String = encode(EncodingContext(op, 1, 0))

  def encode(ctx: EncodingContext[FlowOp]): String = {
    val flowUml = apply(ctx)
    val title   = ctx.op.name.map(x => s"title $x").getOrElse("")
    s"""@startuml
       |start
       |$title
       |$flowUml
       |stop
       |@enduml""".stripMargin
  }

  val action: Encoder[Action] = (ctx: EncodingContext[Action]) => {
    val actionName = ctx.op.name.getOrElse(s"$NoName action")
    s":$actionName;"
  }

  val rule: Encoder[RuleOp] = (ctx: EncodingContext[RuleOp]) => {
    val r                                              = ctx.op
    val ruleName                                       = r.name.getOrElse(s"$NoName rule")
    def conditionOrRuleName(c: Rule.Condition): String = c.name.getOrElse(ruleName)
    def buildConditionName(c: Rule.Condition): String  = c.name.getOrElse(s"$NoName condition")
    def otherwiseName(): Option[String]                = r.otherwise.map(_.name.getOrElse("Otherwise"))

    r.conditions.toList match {
      case head :: Nil =>
        val ruleBuilder = new RuleUmlBuilder().startIf(conditionOrRuleName(head)).action(1)
        otherwiseName().foreach(ruleBuilder.otherwise(_).action(2))
        ruleBuilder.endIf()
      case head :: tail =>
        var actionNum   = 1
        val ruleBuilder = new RuleUmlBuilder().startIf(conditionOrRuleName(head)).action(actionNum)
        tail.foreach { c =>
          actionNum += 1
          ruleBuilder.startElseIf(buildConditionName(c)).action(actionNum)
        }
        otherwiseName().foreach(ruleBuilder.otherwise(_).action(actionNum + 1))
        ruleBuilder.endIf()
      case Nil => ""
    }
  }

  val flow: Encoder[Flow] = (ctx: EncodingContext[Flow]) => {
    ctx.op.ops.map(x => generic(ctx.withOperatorAndDepth(x))).foldLeft("")(String.join("\r\n", _, _))
  }

  val gateway: Encoder[GatewayOp] = (ctx: EncodingContext[GatewayOp]) => {
    val g           = ctx.op
    val gatewayName = g.name.getOrElse(s"$NoName gateway")
    val cases = g.whens.zipWithIndex.map {
      case (when, idx) =>
        val whenName = when.name.getOrElse(s"Condition $idx")
        s"""case ($whenName)
         |${generic(ctx.withOperatorAndDepth(when.op))}""".stripMargin
    } :+
      s"""case (otherwise)
         |${generic(ctx.withOperatorAndDepth(g.otherwise))}""".stripMargin

    val casesString = cases.mkString("\r\n")

    s"""switch ($gatewayName)
       |$casesString
       |endswitch""".stripMargin
  }

  val table: Encoder[TableOp] = (ctx: EncodingContext[TableOp]) => {
    val t    = ctx.op
    val name = t.name.getOrElse(s"$NoName table")
    val r    = new StringBuilder()
    r ++= s":$name;\r\n"
    r ++= "note right\r\n"

    t.expressions.foreach { e =>
      r ++= "||= "
      r ++= e.name
    }
    r ++= "|= action |\r\n"

    t.conditions.foreach { row =>
      val rowBuilder = new StringBuilder()
      row.operators.foreach { operator =>
        rowBuilder ++= "| "
        rowBuilder ++= operator.show()
      }
      r ++= "|"
      r ++= rowBuilder.result()
      r ++= "| "
      row.callableAction.show().foreach { actionName =>
        r ++= actionName
      }
      r ++= " |\r\n"
    }

    r ++= "end note"

    r.result()
  }

  val whileLoop: Encoder[WhileOp] = (ctx: EncodingContext[WhileOp]) => {
    val w        = ctx.op
    val loopName = w.name.getOrElse("Condition is true?")
    val bodyUml  = encoders.generic(ctx.withOperatorAndDepth(w.body))
    s"""while ($loopName) is (yes)
       |$bodyUml
       |endwhile (no)""".stripMargin
  }

  val forEach: Encoder[ForEachOp] = ForEachOperatorEncoder

  val nameOnly: Encoder[FlowOp] = (ctx: EncodingContext[FlowOp]) => {
    val name = ctx.op.name.getOrElse(s"$NoName operator")
    s":$name;"
  }
}

object SimplePlantUmlEncoders extends PlantUmlEncoders

class PlantUmlEncodersImpl(dataGenerator: Class[_] => Any) extends PlantUmlEncoders {
  override val forEach: Encoder[ForEachOp] = ForEachOpDataGeneratingEncoderImpl(dataGenerator, this)
}

private[uml] class RuleUmlBuilder {
  val b = new mutable.StringBuilder()

  def startIf(name: String): RuleUmlBuilder = {
    b ++= s"if ($name) then (yes)"
    this
  }

  def startElseIf(name: String): RuleUmlBuilder = {
    b ++= s"\r\n(no) elseif ($name) then (yes)"
    this
  }

  def action(num: Int): RuleUmlBuilder = {
    b ++= s"\r\n:action$num;"
    this
  }

  def otherwise(name: String): RuleUmlBuilder = {
    b ++= s"\r\nelse ($name)"
    this
  }

  def endIf(): String = {
    b ++= "\r\nendif"
    b.result()
  }
}
