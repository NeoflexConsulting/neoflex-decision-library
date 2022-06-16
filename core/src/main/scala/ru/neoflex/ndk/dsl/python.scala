package ru.neoflex.ndk.dsl

import io.circe.{ Decoder, Encoder }
import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport

final case class PythonOperator[In: Encoder, Out: Decoder](
  override val id: String,
  override val name: Option[String],
  command: String,
  dataIn: () => In,
  resultCollector: Out => Unit)
    extends PythonOperatorOp[In, Out]
    with DeclarationLocationSupport {
  override def isEmbedded: Boolean = true
}

trait PythonOperatorSyntax {
  def pythonCall[In: Encoder, Out: Decoder](
    id: String,
    command: String,
    dataIn: => In
  )(
    collector: Out => Unit
  ): PythonOperator[In, Out] = pythonCall(id, None, command, dataIn)(collector)

  def pythonCall[In: Encoder, Out: Decoder](
    id: String,
    name: Option[String],
    command: String,
    dataIn: => In
  )(
    collector: Out => Unit
  ): PythonOperator[In, Out] = {
    PythonOperator(id, name, command, () => dataIn, collector)
  }
}
