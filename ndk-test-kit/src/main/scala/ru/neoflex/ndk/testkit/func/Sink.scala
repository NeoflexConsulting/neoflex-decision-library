package ru.neoflex.ndk.testkit.func

import akka.stream.scaladsl.{ FileIO, Keep, Flow => AkkaFlow, Sink => AkkaSink }
import akka.util.ByteString
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import doobie.Fragment
import doobie.util.Write
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.{ Encoder, Printer }
import ru.neoflex.ndk.engine.tracking.OperatorTrackedEventRoot
import ru.neoflex.ndk.testkit.func.metric.Metric.MetricValueType
import ru.neoflex.ndk.testkit.func.metric.RunMetrics
import ru.neoflex.ndk.testkit.func.sink.{ BatchedSqlSink, FragmentedSqlSink, JsonArrayWrapStage, SingleSqlSink }

import java.nio.file.Paths
import scala.concurrent.Future

final case class Sink[A](private[func] val s: AkkaSink[A, Future[_]])

object Sink {
  def json[A: Encoder](path: String, singleValue: Boolean = false): Sink[A] = {
    Sink {
      AkkaFlow[A]
        .map(implicitly[Encoder[A]].apply(_).printWith(Printer.spaces2).getBytes)
        .via(new JsonArrayWrapStage(singleValue))
        .map(ByteString.apply)
        .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)
    }
  }

  def sqlMetricsRows(table: String = "metrics_data")(implicit xa: Transactor[IO], ioRuntime: IORuntime): Sink[RunMetrics] = {
    def buildQueries(m: RunMetrics) = {
      m.metrics.toList.map {
        case (name, value) =>
          sql"INSERT INTO " ++ Fragment.const(table) ++ sql"(run_id, name, value) VALUES (${m.runId}, $name, $value)"
      }
    }

    Sink {
      AkkaFlow[RunMetrics]
        .toMat(AkkaSink.fromGraph(new FragmentedSqlSink[RunMetrics](buildQueries)))(Keep.right)
    }
  }

  def sqlMetricsJson(
    table: String = "metrics_json"
  )(implicit xa: Transactor[IO],
    enc: Encoder[Map[String, MetricValueType]] = Encoder.encodeMap[String, MetricValueType],
    ioRuntime: IORuntime
  ): Sink[RunMetrics] = {
    implicit val mw: Write[Map[String, MetricValueType]] = Write[String].contramap { m =>
      enc(m).printWith(Printer.spaces2)
    }
    sqlMetricsJsonWithQuery(s"INSERT INTO $table(run_id, metrics) VALUES (?, ?)")
  }

  def sqlMetricsJsonWithQuery(
    insertQuery: String
  )(implicit xa: Transactor[IO],
    mw: Write[Map[String, MetricValueType]],
    IORuntime: IORuntime
  ): Sink[RunMetrics] = {
    implicit val w: Write[RunMetrics] = Write[String].product(mw).contramap[RunMetrics] { m =>
      (m.runId, m.metrics)
    }
    sqlBatch[RunMetrics](insertQuery)
  }

  def sqlTracesJson(
    table: String = "flow_trace_json"
  )(implicit xa: Transactor[IO],
    enc: Encoder[OperatorTrackedEventRoot],
    IORuntime: IORuntime
  ): Sink[RunFlowTraceEvent] = {
    implicit val ew: Write[OperatorTrackedEventRoot] = Write[String].contramap[OperatorTrackedEventRoot] { e =>
      enc(e).printWith(Printer.spaces2)
    }
    sqlTracesJsonWithQuery(s"INSERT INTO $table(run_id, trace_event) VALUES (?, ?)")
  }

  def sqlTracesJsonWithQuery(
    insertQuery: String
  )(implicit xa: Transactor[IO],
    ew: Write[OperatorTrackedEventRoot],
    IORuntime: IORuntime
  ): Sink[RunFlowTraceEvent] = {
    implicit val w: Write[RunFlowTraceEvent] = Write[String].product(ew).contramap[RunFlowTraceEvent] { e =>
      (e.runId, e.event)
    }
    sqlBatch[RunFlowTraceEvent](insertQuery)
  }

  def sqlBatch[A: Write](
    insertQuery: String,
    batchSize: Int = 500
  )(implicit xa: Transactor[IO],
    ioRuntime: IORuntime
  ): Sink[A] = Sink {
    AkkaFlow[A]
      .toMat(AkkaSink.fromGraph(new BatchedSqlSink[A](insertQuery, batchSize)))(Keep.right)
  }

  def sql[A: Write](insertQuery: String)(implicit xa: Transactor[IO], ioRuntime: IORuntime): Sink[A] = Sink {
    AkkaFlow[A]
      .toMat(AkkaSink.fromGraph(new SingleSqlSink[A](insertQuery)))(Keep.right)
  }

  def ignore[A]: Sink[A] = Sink(AkkaSink.ignore)

  def console[A]: Sink[A] = Sink {
    AkkaSink.foreach[A] { x =>
      println(x)
    }
  }
}
