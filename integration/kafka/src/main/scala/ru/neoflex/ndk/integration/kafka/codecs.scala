package ru.neoflex.ndk.integration.kafka

trait Encoder[A] {
  def encode(value: A): Array[Byte]
}

object Encoder {
  def apply[A: Encoder]: Encoder[A] = implicitly[Encoder[A]]

  implicit def circeEncoderToInternal[A: io.circe.Encoder]: Encoder[A] = value => {
    io.circe.Encoder[A].apply(value).printWith(io.circe.Printer.spaces2).getBytes()
  }
}

trait Decoder[A] {
  def decode(bytes: Array[Byte]): Either[Throwable, A]
}

object Decoder {
  def apply[A: Decoder]: Decoder[A] = implicitly[Decoder[A]]

  implicit def circeDecoderToInternal[A: io.circe.Decoder]: Decoder[A] = bytes => {
    io.circe.parser.decode(new String(bytes))(io.circe.Decoder[A])
  }
}
