package ru.neoflex.ndk.dsl

import cats.implicits.toTraverseOps
import ru.neoflex.ndk.dsl.Gateway.When
import ru.neoflex.ndk.dsl.Rule.{ Condition, Otherwise }
import ru.neoflex.ndk.dsl.Table.{ ActionDef, Expression }
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport
import syntax.NoId

trait Constants {
  val NoId   = "NoId"
  val NoName = "NoName"
}

sealed trait FlowOp {
  def id: String           = NoId
  def name: Option[String] = None

  def isEmbedded: Boolean = false
}

trait Action extends FlowOp with (() => Unit) with DeclarationLocationSupport {
  val f: () => Unit
}

trait RuleOp extends FlowOp {
  def conditions: Seq[Condition]
  def otherwise: Option[Otherwise]
}

abstract class Flow(override val id: String, val ops: Seq[FlowOp], override val name: Option[String] = None)
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
}
final case class SealedFlow(override val id: String, override val name: Option[String], override val ops: Seq[FlowOp])
    extends Flow(id, name, ops) {

  def apply(ops: FlowOp*): SealedFlow = copy(ops = ops)

  override def isEmbedded: Boolean = true
}

trait TableOp extends FlowOp {
  val expressions: List[Expression]
  val actions: List[ActionDef]
  val conditions: List[Table.Condition]
  val actionsByName: Map[String, ActionDef]
}

trait GatewayOp extends FlowOp {
  val whens: Seq[When]
  val otherwise: FlowOp
}

trait WhileOp extends FlowOp {
  def condition: () => Boolean
  def body: FlowOp
}

trait ForEachOp extends FlowOp {
  def collection: () => Iterable[Any]
  def body: Any => FlowOp
  def elementClass: Option[Class[_]]
}

abstract class PythonOperatorOp[In: PyDataEncoder, Out: PyDataDecoder] extends FlowOp {
  def command: String
  def dataIn: () => Seq[In]
  def resultCollector: Seq[Out] => Unit

  def encodedDataIn: Iterator[String] = dataIn().iterator.map { v =>
    implicitly[PyDataEncoder[In]].encode(v)
  }

  def collectResults(strings: Seq[String]): Either[Throwable, Unit] = {
    strings.map { v =>
      implicitly[PyDataDecoder[Out]].decode(v)
    }.toList.sequence.map(resultCollector).map(_ => ())
  }
}

trait PyDataEncoder[T] {
  def encode(v: T): String
}

trait PyDataDecoder[T] {
  def decode(v: String): Either[Throwable, T]
}

trait PyDataCodec[T] extends PyDataEncoder[T] with PyDataDecoder[T]

trait FlowSyntax {
  def flow: SealedFlow                                          = SealedFlow(NoId, None, Seq.empty)
  def flow(id: String, name: Option[String] = None): SealedFlow = SealedFlow(id, name, Seq.empty)
  def flowOps(ops: FlowOp*): Seq[FlowOp]                        = ops
}
