package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.declaration.DeclarationLocationSupport

import scala.util.Try

final case class PythonOperator[In: PyDataEncoder, Out: PyDataDecoder](
  override val id: String,
  override val name: Option[String],
  command: String,
  dataIn: () => Seq[In],
  resultCollector: Seq[Out] => Unit)
    extends PythonOperatorOp[In, Out] with DeclarationLocationSupport {
  override def isEmbedded: Boolean = true
}

trait PythonOperatorSyntax {
  def pythonCall[In: PyDataEncoder, Out: PyDataDecoder](
    id: String,
    command: String,
    dataIn: => Seq[In]
  )(
    collector: Seq[Out] => Unit
  ): PythonOperator[In, Out] = pythonCall(id, None, command, dataIn)(collector)

  def singleParameterPythonCall[In: PyDataEncoder, Out: PyDataDecoder](
    id: String,
    command: String,
    dataIn: => In
  )(
    collector: Out => Unit
  ): PythonOperator[In, Out] = pythonCall(id, command, Seq(dataIn))(_.foreach(collector))

  def pythonCall[In: PyDataEncoder, Out: PyDataDecoder](
    id: String,
    name: Option[String],
    command: String,
    dataIn: => Seq[In]
  )(
    collector: Seq[Out] => Unit
  ): PythonOperator[In, Out] = {
    PythonOperator(id, name, command, () => dataIn, collector)
  }
}

trait PyDataCodecImplicits {
  trait ToStringEncoder[T] extends PyDataEncoder[T] {
    def encode(v: T): String = v.toString
  }
  abstract class BaseDecoder[T](f: String => T) extends PyDataDecoder[T] {
    def decode(v: String): Either[Throwable, T] = Try(f(v)).toEither
  }

  implicit val booleanCodec: PyDataCodec[Boolean] =
    new BaseDecoder[Boolean](_.toBoolean) with ToStringEncoder[Boolean] with PyDataCodec[Boolean]

  implicit val byteCodec: PyDataCodec[Byte] =
    new BaseDecoder[Byte](_.toByte) with ToStringEncoder[Byte] with PyDataCodec[Byte]

  implicit val shortCodec: PyDataCodec[Short] =
    new BaseDecoder[Short](_.toShort) with ToStringEncoder[Short] with PyDataCodec[Short]

  implicit val charCodec: PyDataCodec[Char] =
    new BaseDecoder[Char](_.charAt(0)) with ToStringEncoder[Char] with PyDataCodec[Char]

  implicit val floatCodec: PyDataCodec[Float] =
    new BaseDecoder[Float](_.toFloat) with ToStringEncoder[Float] with PyDataCodec[Float]

  implicit val doubleCodec: PyDataCodec[Double] =
    new BaseDecoder[Double](_.toDouble) with ToStringEncoder[Double] with PyDataCodec[Double]

  implicit val intCodec: PyDataCodec[Int] =
    new BaseDecoder[Int](_.toInt) with ToStringEncoder[Int] with PyDataCodec[Int]

  implicit val longCodec: PyDataCodec[Long] =
    new BaseDecoder[Long](_.toLong) with ToStringEncoder[Long] with PyDataCodec[Long]

  implicit val stringCodec: PyDataCodec[String] = new PyDataCodec[String] {
    override def decode(v: String): Either[Throwable, String] = Right(v)
    override def encode(v: String): String                    = v
  }
}
