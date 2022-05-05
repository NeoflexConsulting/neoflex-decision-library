package ru.neoflex.ndk.renderer.uml

import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.renderer.{ Encoder, Encoders, GenericEncoder }

import scala.collection.mutable

trait PlantUmlEncoders extends Encoders with Constants with GenericEncoder {
  override val encoders: Encoders       = this
  override def generic: Encoder[FlowOp] = this

  def encode(op: FlowOp): String = {
    val flowUml = apply(op)
    s"""@startuml
       |start
       |$flowUml
       |stop
       |@enduml""".stripMargin
  }

  val action: Encoder[Action] = (a: Action) => {
    val actionName = a.name.getOrElse(s"$NoName action")
    s":$actionName;"
  }

  val rule: Encoder[RuleOp] = (r: RuleOp) => {
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

  val flow: Encoder[Flow] = (f: Flow) => {
    f.ops.map(generic).foldLeft("")(String.join("\r\n", _, _))
  }

  val gateway: Encoder[GatewayOp] = (g: GatewayOp) => {
    val gatewayName = g.name.getOrElse(s"$NoName gateway")
    val cases = g.whens.zipWithIndex.map {
      case (when, idx) =>
        val whenName = when.name.getOrElse(s"Condition $idx")
        s"""case ($whenName)
         |${generic(when.op)}""".stripMargin
    } :+
      s"""case (otherwise)
         |${generic(g.otherwise)}""".stripMargin

    val casesString = cases.mkString("\r\n")

    s"""switch ($gatewayName)
       |$casesString
       |endswitch""".stripMargin
  }

  val table: Encoder[TableOp] = (t: TableOp) => {
    t.name.getOrElse(s":$NoName table;")
  }

  override def whileLoop: Encoder[WhileOp] = (w: WhileOp) => {
    val loopName = w.name.getOrElse("Condition is true?")
    val bodyUml  = encoders.generic(w.body)
    s"""while ($loopName) is (yes)
       |$bodyUml
       |endwhile (no)""".stripMargin
  }

  override def forEach: Encoder[ForEachOp] = (f: ForEachOp) => {
    val loopName = f.name.getOrElse("has more elements?")
    s"""
       |while ($loopName) is (yes)
       |:action;
       |endwhile (no)""".stripMargin
  }
}

object PlantUmlEncoders extends PlantUmlEncoders

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
