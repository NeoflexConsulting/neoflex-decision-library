package ru.neoflex.ndk.dsl

import io.circe.{ Decoder, Encoder, Json, Printer }
import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.Rule.{ Condition, Otherwise }
import ru.neoflex.ndk.dsl.Table.{ ActionDef, Expression }
import ru.neoflex.ndk.dsl.`type`.OperatorType
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
import ru.neoflex.ndk.dsl.syntax.NoId

import scala.util.control.Exception.nonFatalCatch

trait Constants {
  val NoId   = "NoId"
  val NoName = "NoName"
}

sealed trait FlowOp {
  def id: String           = NoId
  def name: Option[String] = None

  /**
   * User defined unique identifier of the entity at the flow input
   */
  def entityId: Option[String] = None

  def isEmbedded: Boolean = false

  def operatorType: OperatorType
}

trait Action extends FlowOp with (() => Unit) with DeclarationLocationSupport {
  val f: () => Unit

  final override def operatorType: OperatorType = OperatorType.Action
}

trait RuleOp extends FlowOp {
  def conditions: Seq[Condition]
  def otherwise: Option[Otherwise]
  final override def operatorType: OperatorType = OperatorType.Rule
}

abstract class Flow(
  override val id: String,
  val ops: Seq[FlowOp],
  override val name: Option[String] = None,
  override val entityId: Option[String] = None)
    extends FlowOp {
  def this(id: String, op: FlowOp) = {
    this(id, Seq(op))
  }

  def this(id: String, name: Option[String], op: FlowOp) = {
    this(id, Seq(op), name)
  }

  def this(id: String, name: Option[String], ops: Seq[FlowOp]) = {
    this(id, ops, name)
  }

  def this(id: String, name: Option[String], entityId: Option[String], ops: Seq[FlowOp]) = {
    this(id, ops, name, entityId)
  }

  final override def operatorType: OperatorType = OperatorType.Flow
}
final case class SealedFlow(
  override val id: String,
  override val name: Option[String],
  override val entityId: Option[String],
  override val ops: Seq[FlowOp])
    extends Flow(id, name, entityId, ops) {

  def apply(ops: FlowOp*): SealedFlow = copy(ops = ops)

  override def isEmbedded: Boolean = true
}

trait TableOp extends FlowOp {
  val expressions: List[Expression]
  val actions: List[ActionDef]
  val conditions: List[Table.Condition]
  val actionsByName: Map[String, ActionDef]
  final override def operatorType: OperatorType = OperatorType.Table
}

trait GatewayOp extends FlowOp {
  val whens: Seq[When]
  val otherwise: FlowOp
  final override def operatorType: OperatorType = OperatorType.Gateway
}

trait WhileOp extends FlowOp {
  def condition: () => Boolean
  def body: FlowOp
  final override def operatorType: OperatorType = OperatorType.While
}

trait ForEachOp extends FlowOp {
  def collection: () => Iterable[Any]
  def body: Any => FlowOp
  def elementClass: Option[Class[_]]
  final override def operatorType: OperatorType = OperatorType.ForEach
}

abstract class RestService[Req: Encoder, Resp: Decoder] extends FlowOp {
  def serviceNameOrEndpoint: RestService.ServiceNameOrEndpoint
  def makeUri: () => String
  def inputBody: () => Option[Req]
  def encodedInputBody: Option[Json] = inputBody().map { implicitly[Encoder[Req]].apply(_) }
  def responseCollector: Resp => Unit
  def collectResponse(body: Option[String]): Either[Throwable, Unit] = {
    body.map { response =>
      val decoder = implicitly[Decoder[Resp]]
      io.circe.jawn.decode(response)(decoder).map(responseCollector)
    }.getOrElse(Right(()))
  }
  final override def operatorType: OperatorType = OperatorType.RestService
}

object RestService {
  type ServiceNameOrEndpoint = Either[String, String]
}

abstract class PythonOperatorOp[In: Encoder, Out: Decoder] extends FlowOp {
  def command: String
  def dataIn: () => In
  def resultCollector: Out => Unit

  def encodedDataIn: Either[Throwable, String] =
    nonFatalCatch.either {
      Encoder[In].apply(dataIn()).printWith(Printer.noSpaces)
    }

  def collectResults(value: String): Either[Throwable, Unit] = {
    io.circe.jawn.decode[Out](value)(Decoder[Out]).map(resultCollector)
  }

  final override def operatorType: OperatorType = OperatorType.PyOperator
}

trait FlowSyntax {
  def flow: SealedFlow = SealedFlow(NoId, None, None, Seq.empty)
  def flow(id: String, name: Option[String] = None, entityId: Option[String] = None): SealedFlow =
    SealedFlow(id, name, entityId, Seq.empty)

  def flowOps(ops: FlowOp*): Seq[FlowOp] = ops
}
