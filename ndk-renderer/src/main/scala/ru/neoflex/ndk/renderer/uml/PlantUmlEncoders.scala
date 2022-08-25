package ru.neoflex.ndk.renderer.uml

import cats.implicits.catsSyntaxOptionId
import ru.neoflex.ndk.NdkKeywords
import ru.neoflex.ndk.dsl.Rule.Otherwise
import ru.neoflex.ndk.dsl._
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
import ru.neoflex.ndk.dsl.dictionary.DictionaryValue
import ru.neoflex.ndk.dsl.implicits.CallableActionOps
import ru.neoflex.ndk.dsl.syntax.OperatorOps
import ru.neoflex.ndk.renderer.{ DepthLimitedEncoder, Encoder, Encoders, EncodingContext }

import scala.collection.mutable
import scala.util.Try

trait PlantUmlEncoders extends Encoders with Constants with DepthLimitedEncoder with LoopUmlBuilder {
  override val encoders: Encoders       = this
  override def generic: Encoder[FlowOp] = this

  def encode(op: FlowOp): String = encode(EncodingContext(op, 1, 0))

  def encode(ctx: EncodingContext[FlowOp]): String = {
    val flowUml = apply(ctx)
    val title   = ctx.op.name.getOrElse(s"$NoName flow")
    new UmlBuilder()
      .startUml()
      .start()
      .title(title)
      .add(flowUml)
      .stop()
      .endUml()
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

  private def toDictionaryLinkDirective(dv: DictionaryValue[_]): String = {
    val link = DictionaryLink(dv.dictionaryName, DictionaryValue.DictFileExtension, dv.key)
    s""" [${NdkKeywords.UmlNdkData}:${link.stringValue}]"""
  }

  private def toLinkReprIfDictCondition(c: LazyCondition): String = c match {
    case DictLazyCondition(v, _) => toDictionaryLinkDirective(v)
    case _                       => ""
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
    def appendDictLinkIfExists(c: Rule.Condition)(buildName: Rule.Condition => String): String = {
      val name = buildName(c)
      name + toLinkReprIfDictCondition(c.expr)
    }
    def startIf(c: Rule.Condition): RuleUmlBuilder = {
      val builder = new RuleUmlBuilder()
      val name    = appendDictLinkIfExists(c)(conditionOrRuleName)
      builder.startIf(name).action(1, makeLinkOrEmpty(c))
    }

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
          ruleBuilder.startElseIf(appendDictLinkIfExists(c)(buildConditionName)).action(actionNum, makeLinkOrEmpty(c))
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
    val g              = ctx.op
    val gatewayName    = g.name.getOrElse(s"$NoName gateway")
    val gatewayBuilder = new GatewayUmlBuilder().startSwitch(gatewayName)

    g.whens.zipWithIndex.foreach {
      case (when, idx) =>
        val whenName = when.name.getOrElse(s"Condition $idx") + toLinkReprIfDictCondition(when.cond)
        gatewayBuilder.addCase(whenName, generic(ctx.withOperatorAndDepth(when.op)))
    }

    gatewayBuilder
      .otherwise("otherwise", generic(ctx.withOperatorAndDepth(g.otherwise)))
      .endSwitch()
  }

  val table: Encoder[TableOp] = (ctx: EncodingContext[TableOp]) => {
    def toCaseName(e: Table.Expression): String = {
      val caseName = e.name
      Try(e.f()).map {
        case dv: DictionaryValue[_] => s"$caseName ${toDictionaryLinkDirective(dv)}"
        case _                      => caseName
      }.toOption.getOrElse(caseName)
    }

    val t    = ctx.op
    val name = t.name.getOrElse(s"$NoName table")
    val r    = new mutable.StringBuilder()
    r ++= s":$name;\r\n"
    r ++= "note right\r\n"

    t.expressions.foreach { e =>
      r ++= "|= "
      r ++= toCaseName(e)
    }
    t.expressions.headOption.foreach { _ =>
      r ++= "|= action |\r\n"
    }

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
    buildLoopUml(loopName, bodyUml)
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

private[uml] class UmlBuilder {
  val b = new mutable.StringBuilder()

  def startUml(): UmlBuilder = {
    b ++= "@startuml"
    this
  }

  def endUml(): String = {
    b ++= "\r\n@enduml"
    b.result()
  }

  def title(name: String): UmlBuilder = {
    b ++= "\r\ntitle "
    b ++= name
    this
  }

  def add(text: String): UmlBuilder = {
    b ++= "\r\n"
    b ++= text
    this
  }

  def start(): UmlBuilder = {
    b ++= "\r\nstart"
    this
  }

  def stop(): UmlBuilder = {
    b ++= "\r\nstop"
    this
  }
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

private[uml] class GatewayUmlBuilder {
  val b = new mutable.StringBuilder()

  def startSwitch(name: String): GatewayUmlBuilder = {
    b ++= "switch ("
    b ++= name
    b ++= ")"
    this
  }

  def addCase(name: String, body: String): GatewayUmlBuilder = {
    b ++= "\r\ncase ("
    b ++= name
    b ++= ")\r\n"
    b ++= body
    this
  }

  def otherwise(name: String, body: String): GatewayUmlBuilder = {
    b ++= "\r\ncase ("
    b ++= name
    b ++= ")\r\n"
    b ++= body
    this
  }

  def endSwitch(): String = {
    b ++= "\r\nendswitch"
    b.result()
  }
}
