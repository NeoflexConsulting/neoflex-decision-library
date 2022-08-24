package ru.neoflex.ndk.testkit.func

import doobie.implicits._
import io.circe.generic.auto._
import org.scalatest.EitherValues
import org.scalatest.enablers.Existence
import org.scalatest.matchers.should.Matchers
import ru.neoflex.ndk.dsl.Flow
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.testkit.func.implicits._
import ru.neoflex.ndk.testkit.func.metric.RunMetrics

import java.nio.file.{ Files, Paths }
import scala.util.Random

class FuncTestDslSpec extends NdkFuncSpec with SqlDbBinders with EitherValues with Matchers {
  override protected def datasourceConfig: DataSourceConfig = DataSourceConfig(
    "org.h2.Driver",
    "jdbc:h2:~/ndk-test",
    "sa",
    "sa"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()

    sql"DROP ALL OBJECTS".update.run.transact(xa).unsafeRunSync()

    sql"""CREATE TABLE metrics_data(
            run_id character varying,
            metric_name character varying,
            metric_value numeric
         )""".update.run.transact(xa).unsafeRunSync()

    sql"""CREATE TABLE flow_run_result(
            run_id character varying,
            age integer,
            executed bool,
            status character varying,
            sex char
          )
         """.update.run.transact(xa).unsafeRunSync()

    sql"CREATE TABLE flow_trace_json(run_id character varying, trace_event character varying)".update.run
      .transact(xa)
      .unsafeRunSync()

    sql"CREATE TABLE executed_flow_result(run_id character varying, status character varying, sex char)".update.run
      .transact(xa)
      .unsafeRunSync()
  }

  implicit class StringAsPath(s: String) {
    def resourcePath: String = getClass.getResource(s"/$s").getFile
  }

  private implicit val filePathExistence: Existence[String] = (thing: String) => Files.exists(Paths.get(thing))

  "json source pipeline" should "be resulted in sql metrics and json traces" in {
    Source
      .json[SimpleData]("input_data.json".resourcePath)
      .filter(_.age >= 18)
      .map(SimpleFlow)
      .result
      .map(_.result.data)
      .filter(x => x.executed)
      .withMetrics {
        _.metric("cover").as { result =>
          result
            .filter(_.status == "APPROVED")
            .count() / result.totalElements * 100
        }.metric("men_to_women")
          .as { result =>
            result.filter(_.sex == "M").count() / result.filter(_.sex == "W").count()
          }
          .metric("approved")
          .as {
            _.filter(_.status == "APPROVED").count()
          }
          .metric("declined")
          .as {
            _.filter(_.status == "DECLINED").count()
          }
          .metric("men_approved_perc")
          .as { result =>
            result.filter(_.sex == "M").filter(_.status == "APPROVED").count() / result.totalElements
          }
          .metric("women_approved_perc")
          .as { result =>
            result.filter(_.sex == "W").filter(_.status == "APPROVED").count() / result.totalElements
          }
          .metric("avg_approved")
          .as { result =>
            result.filter(_.status == "APPROVED").avg(_ => 1)
          }
          .toSink(Sink.sqlMetricsRows())
      }
      .withFlowTraceSink(Sink.json[RunFlowTraceEvent]("/tmp/traces.json"))
      .runWithSink(Sink.console[SimpleData])
      .awaitResult()

    "/tmp/traces.json" should exist
  }

  "json source pipeline" should "be resulted in table with json metrics and flow data in flat table" in {
    val runId = Source
      .json[SimpleData]("input_data.json".resourcePath)
      .filter(_.age != 10)
      .map(SimpleFlow)
      .result
      .map(r => r.copy(result = r.result.data))
      .withFlowTraceSink(Sink.sqlTracesJson())
      .runWithSink(
        Sink.sqlBatch[RunFlowResult[SimpleData]](
          "INSERT INTO flow_run_result(run_id, age, executed, status, sex) VALUES (?, ?, ?, ?, ?)"
        )
      )
      .awaitResult()

    val insertedRows =
      sql"SELECT count(*) FROM flow_run_result WHERE run_id = $runId".query[Long].unique.transact(xa).unsafeRunSync()

    insertedRows should be(7)
  }

  "json source pipeline" should "be resulted in data saved with custom sql into table" in {
    val runId = Source
      .json[SimpleData]("input_data.json".resourcePath)
      .filter(_.executed == false)
      .map(SimpleFlow)
      .result
      .map(r => r.copy(result = r.result.data))
      .filter(_.result.executed)
      .withMetrics {
        _.metric("cover").as { r =>
          r.filter(_.result.status == "APPROVED").count() / r.totalElements * 100
        }.metric("declined")
          .as {
            _.filter(_.result.status == "DECLINED").count()
          }
          .toSink(
            Sink.sql { m: RunMetrics =>
              m.metrics.toList.map {
                case (name, value) =>
                  sql"INSERT INTO metrics_data VALUES (${m.runId}, $name, $value)"
              }
            }
          )
      }
      .runWithSink(
        Sink.sql { d: RunFlowResult[SimpleData] =>
          sql"INSERT INTO executed_flow_result(run_id, status, sex) VALUES (${d.runId}, ${d.result.status}, ${d.result.sex})" :: Nil
        }
      )
      .awaitResult()

    val insertedMetricsRows =
      sql"SELECT count(*) FROM metrics_data WHERE run_id = $runId".query[Long].unique.transact(xa).unsafeRunSync()

    val insertedResultRows =
      sql"SELECT count(*) FROM executed_flow_result WHERE run_id = $runId".query[Long].unique.transact(xa).unsafeRunSync()

    insertedMetricsRows should be (2)
    insertedResultRows should be (7)
  }

  "json source pipeline" should "be resulted in data inserted into custom table" in {
    val runId = Source
      .json[SimpleData]("input_data.json".resourcePath)
      .filter(_.age > 15)
      .map(SimpleFlow)
      .result
      .filter(_.result.data.executed)
      .map(r => (r.runId, r.result.data.status, r.result.data.sex))
      .runWithSink(Sink.sqlTable("executed_flow_result", 3))
      .awaitResult()

    val insertedResultRows =
      sql"SELECT count(*) FROM executed_flow_result WHERE run_id = $runId".query[Long].unique.transact(xa).unsafeRunSync()

    insertedResultRows should be (7)
  }

  "sql table source pipeline" should "stream data to flow and save result in the custom table" in {
    Source
      .json[SimpleData]("input_data.json".resourcePath)
      .map(SimpleFlow)
      .result
      .map(r => r.copy(result = r.result.data))
      .withFlowTraceSink(Sink.sqlTracesJson())
      .runWithSink(Sink.sqlTable("flow_run_result", 5))
      .awaitResult()

    val runId = Source
      .sqlTable[RunFlowResult[SimpleData]]("flow_run_result")
      .map(_.result)
      .map(_.copy(executed = false, status = ""))
      .map(SimpleFlow)
      .result
      .filter(_.result.data.executed)
      .map(r => (r.runId, r.result.data.status, r.result.data.sex))
      .runWithSink(Sink.sqlTable("executed_flow_result", 3))
      .awaitResult()

    val insertedResultRows =
      sql"SELECT count(*) FROM executed_flow_result WHERE run_id = $runId".query[Long].unique.transact(xa).unsafeRunSync()

    insertedResultRows > 0 should be (true)
  }

  "sql source" should "stream data to flow" in {
    val initialRunId = Source
      .json[SimpleData]("input_data.json".resourcePath)
      .map(SimpleFlow)
      .result
      .map(r => r.copy(result = r.result.data))
      .withFlowTraceSink(Sink.sqlTracesJson())
      .runWithSink(Sink.sqlTable("flow_run_result", 5))
      .awaitResult()

    Source
      .sqlQuery[RunFlowResult[SimpleData]](s"SELECT * FROM flow_run_result WHERE run_id = '$initialRunId'")
      .map(_.result)
      .map(SimpleFlow)
      .result
      .runWithSink(Sink.console)
      .awaitResult()

    val insertedInitialRows =
      sql"SELECT count(*) FROM flow_run_result WHERE run_id = $initialRunId".query[Long].unique.transact(xa).unsafeRunSync()

    insertedInitialRows > 0 should be (true)
  }

  "csv source" should "stream data to csv file" in {
    Source
      .csv[SimpleData]("input_data_with_headers.csv".resourcePath, withHeaders = true)
      .map(SimpleFlow)
      .result
      .map(r => (r.runId, r.result.data.sex, r.result.data.status))
      .runWithSink(
        Sink
          .csv("/tmp/result-data.csv", headers = List("run_id", "sex", "status"))
      )
      .awaitResult()

    "/tmp/result-data.csv" should exist
  }
}

final case class SimpleData(var age: Int = -1, var executed: Boolean = false, var status: String = "", sex: String)
final case class SimpleFlow(data: SimpleData)
    extends Flow(
      "sf-1",
      flowOps(
        rule("sr-1", "status rule") {
          condition(Random.nextBoolean()) andThen {
            data.status = "APPROVED"
          } otherwise {
            data.status = "DECLINED"
          }
        },
        action {
          data.executed = true
        }
      )
    )
