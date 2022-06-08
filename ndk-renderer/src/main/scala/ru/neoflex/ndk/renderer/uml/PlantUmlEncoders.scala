package ru.neoflex.ndk.renderer.uml

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.dsl.Rule.Otherwise
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
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
    addLink(ctx.op, s":$actionName;")
  }

  val pythonOperator: Encoder[PythonOperatorOp[Any, Any]] = (ctx: EncodingContext[PythonOperatorOp[Any, Any]]) => {
    val name = ctx.op.name.getOrElse(ctx.op.command.take(32))
    addLink(ctx.op, s":$name;")
  }

  val restService: Encoder[RestService[Any, Any]] = (ctx: EncodingContext[RestService[Any, Any]]) => {
    val name = ctx.op.name.orElse {
      ctx.op.serviceNameOrEndpoint match {
        case Left(value) => value.some
        case Right(_)    => None
      }
    }.getOrElse(s"$NoName service call")
    addLink(ctx.op, s":$name;")
  }

  val rule: Encoder[RuleOp] = (ctx: EncodingContext[RuleOp]) => {
    val r                                              = ctx.op
    val ruleName                                       = r.name.getOrElse(s"$NoName rule")
    def conditionOrRuleName(c: Rule.Condition): String = c.name.getOrElse(ruleName)
    def buildConditionName(c: Rule.Condition): String  = c.name.getOrElse(s"$NoName condition")
    def otherwiseName(o: Otherwise): String            = o.name.getOrElse("Otherwise")
    def otherwise(rb: RuleUmlBuilder, actionNum: Int): Unit = r.otherwise.foreach { o =>
      rb.otherwise(otherwiseName(o)).action(actionNum, makeLinkOrEmpty(o))
    }
    def startIf(c: Rule.Condition): RuleUmlBuilder =
      new RuleUmlBuilder().startIf(conditionOrRuleName(c)).action(1, makeLinkOrEmpty(c))

    r.conditions.toList match {
      case head :: Nil =>
        val ruleBuilder = startIf(head)
        otherwise(ruleBuilder, 2)
        ruleBuilder.endIf()
      case head :: tail =>
        var actionNum   = 1
        val ruleBuilder = startIf(head)
        tail.foreach { c =>
          actionNum += 1
          ruleBuilder.startElseIf(buildConditionName(c)).action(actionNum, makeLinkOrEmpty(c))
        }
        otherwise(ruleBuilder, actionNum + 1)
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
    val r    = new mutable.StringBuilder()
    r ++= s":$name;\r\n"
    r ++= "note right\r\n"

    t.expressions.foreach { e =>
      r ++= "||= "
      r ++= e.name
    }
    r ++= "|= action |\r\n"

    t.conditions.foreach { row =>
      val rowBuilder = new mutable.StringBuilder()
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

    val uml = r.result()
    addLink(t, uml)
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
    addLink(ctx.op, s":$name;")
  }

  private def makeLinkOrEmpty(dls: DeclarationLocationSupport): String = {
    dls.declarationLocation.map { loc =>
      val link = FileLineNumber(loc.fileName, loc.lineNumber).stringValue
      s"[[$link]]"
    }.getOrElse("")
  }

  private def addLink(op: FlowOp, uml: String): String = {
    if (op.isEmbedded) {
      op match {
        case dls: DeclarationLocationSupport => makeLinkOrEmpty(dls) + uml
        case _                               => uml
      }
    } else {
      val link = ClassLink(op.getClass.getName).stringValue
      s"[[$link]] $uml"
    }
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

  def action(num: Int, link: String): RuleUmlBuilder = {
    b ++= s"\r\n$link:action$num;"
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
