package ru.neoflex.ndk.testkit.func

import akka.stream.scaladsl.{FileIO, JsonFraming, Flow => StreamFlow, Source => StreamSource}
import ru.neoflex.ndk.dsl.FlowOp

import java.nio.file.Paths

final case class Source[A](s: StreamSource[A, _])

object Source {
/*  def csv[A: Codec](path: String,
                    delimiter: Byte = Comma,
                    quoteChar: Byte = DoubleQuote,
                    escapeChar: Byte = Backslash,
                    maximumLineLength: Int = maximumLineLengthDefault): Source[A] = Source {
    FileIO.fromPath(Paths.get(path))
      .via(CsvParsing.lineScanner(delimiter, quoteChar, escapeChar, maximumLineLength))
      .map(_.map(_.utf8String))
      .map(_ => null.asInstanceOf[A])
  }*/

  def json[A: Codec](path: String): Source[A] = Source {
    FileIO
      .fromPath(Paths.get(path))
      .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
      .map { bs =>
        implicitly[Codec[A]].decode(bs.toArrayUnsafe()).fold(throw _, x => x)
      }
  }

//  def sqlQuery[A: Codec](query: String): Source[A] = ???

//  def sqlTable[A: Codec](table: String): Source[A] = ???

  implicit class SourceOps[A](source: Source[A]) {
    def filter(f: A => Boolean): Source[A] = Source(source.s.filter(f))
    def map[B](f: A => B): Source[B]       = Source(source.s.map(f))
    def result[B <: FlowOp](implicit ev: A =:= B): FlowExecutionResult[B, B] =
      FlowExecutionResult[B, B](source.s.asInstanceOf[StreamSource[B, _]], StreamFlow[B])
  }
}
