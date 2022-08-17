package ru.neoflex.ndk.testkit.func.sink

import akka.Done
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler }
import akka.stream.{ Attributes, Inlet, SinkShape }
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import doobie.implicits._
import doobie.util.Write
import doobie.util.fragment.Fragment
import doobie.{ Transactor, Update }
import ru.neoflex.ndk.tools.Logging

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ Future, Promise }

class BatchedSqlSink[A: Write] private[func] (
  queryInitializer: A => String,
  batchSize: Int
)(implicit val xa: Transactor[IO],
  IORuntime: IORuntime)
    extends GraphStageWithMaterializedValue[SinkShape[A], Future[Done]]
    with Logging {

  def this(sql: String, batchSize: Int)(implicit xa: Transactor[IO], IORuntime: IORuntime) = {
    this((_: A) => sql, batchSize)
  }

  private val in                   = Inlet[A]("BatchedSqlSink.in")
  override val shape: SinkShape[A] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()
    val logic = new GraphStageLogic(shape) with InHandler {
      private val buf     = new ArrayBuffer[A](batchSize)
      private var _update = Option.empty[Update[A]]

      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        buf += grab(in)
        if (buf.size >= batchSize) {
          saveBatch()
        }
        pull(in)
      }

      private def getOrCreateUpdateQuery(a: A): Update[A] = {
        _update.getOrElse {
          val querySql = queryInitializer(a)
          val u        = Update[A](querySql)
          _update = Some(u)
          u
        }
      }

      private def saveBatch(): Unit = {
        val update       = getOrCreateUpdateQuery(buf.head)
        val batch        = buf.toList
        val updated      = update.updateMany(batch)
        val withTransact = updated.transact(xa)
        val rowsAffected = withTransact.unsafeRunSync()
        logger.trace(s"$rowsAffected affected by query: {}", update.sql)
        buf.clear()
      }

      override def onUpstreamFinish(): Unit = {
        if (buf.nonEmpty) {
          saveBatch()
        }
      }

      override def postStop(): Unit = promise.success(Done)

      setHandler(in, this)
    }

    logic -> promise.future
  }
}

class SingleSqlSink[A: Write](sql: A => String)(implicit xa: Transactor[IO], ioRuntime: IORuntime)
    extends GraphStageWithMaterializedValue[SinkShape[A], Future[Done]] {
  private val in                   = Inlet[A]("SingleSqlSink.in")
  override val shape: SinkShape[A] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()
    val logic = new GraphStageLogic(shape) with InHandler {
      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val e      = grab(in)
        val update = Update[A](sql(e))
        update.run(e).transact(xa).unsafeRunSync()
        pull(in)
      }

      override def postStop(): Unit = promise.success(Done)

      setHandler(in, this)
    }
    logic -> promise.future
  }
}

class FragmentedSqlSink[A](f: A => List[Fragment])(implicit xa: Transactor[IO], ioRuntime: IORuntime)
    extends GraphStageWithMaterializedValue[SinkShape[A], Future[Done]] {
  private val in                   = Inlet[A]("FragmentedSqlSink.in")
  override val shape: SinkShape[A] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()
    val logic = new GraphStageLogic(shape) with InHandler {
      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val fragments = f(grab(in))
        fragments.foreach { fragment =>
          fragment.update.run.transact(xa).unsafeRunSync()
        }

        pull(in)
      }

      override def postStop(): Unit = promise.success(Done)

      setHandler(in, this)
    }
    logic -> promise.future
  }
}
