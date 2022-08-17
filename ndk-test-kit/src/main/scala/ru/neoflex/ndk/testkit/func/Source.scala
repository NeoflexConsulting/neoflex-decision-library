package ru.neoflex.ndk.testkit.func

import akka.stream.scaladsl.{ FileIO, JsonFraming, Flow => AkkaFlow, Source => AkkaSource }
import cats.effect._
import cats.effect.unsafe.IORuntime
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all._
import doobie.util.Read
import doobie.util.transactor.Transactor
import io.circe.Decoder
import io.circe.parser.decode
import ru.neoflex.ndk.dsl.FlowOp
import ru.neoflex.ndk.testkit.func.source.Fs2StreamAdapter

import java.nio.file.Paths

final case class Source[A](s: AkkaSource[A, _])

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

  def json[A: Decoder](path: String): Source[A] = Source {
    FileIO
      .fromPath(Paths.get(path))
      .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
      .map { bs =>
        decode(new String(bs.toArrayUnsafe()))
          .fold(throw _, j => j)
      }
  }

  def sqlTable[A: Read](table: String)(implicit xa: Transactor[IO], ior: IORuntime): Source[A] =
    sqlQuery(s"SELECT * FROM $table")

  def sqlQuery[A: Read](query: String)(implicit xa: Transactor[IO], ioRuntime: IORuntime): Source[A] = Source {
    val stream = StringContext(query).sql().query[A].stream.transact(xa)
    Fs2StreamAdapter.streamToSource(stream)
  }

  implicit class SourceOps[A](source: Source[A]) {
    def filter(f: A => Boolean): Source[A] = Source(source.s.filter(f))
    def map[B](f: A => B): Source[B]       = Source(source.s.map(f))
    def result[B <: FlowOp](implicit ev: A =:= B): FlowExecutionResult[B, RunFlowResult[B]] =
      FlowExecutionResult[B, RunFlowResult[B]](source.s.asInstanceOf[AkkaSource[B, _]], AkkaFlow[RunFlowResult[B]])
  }
}
