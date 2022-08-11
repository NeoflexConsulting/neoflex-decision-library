package ru.neoflex.ndk.testkit.func

import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.scaladsl.{ FileIO, Keep, Flow => StreamFlow, Sink => StreamSink }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.util.ByteString

import java.nio.file.Paths
import scala.concurrent.Future

final case class Sink[A](private[func] val s: StreamSink[A, Future[_]])

object Sink {
  def json[A: Codec](path: String, singleValue: Boolean = false): Sink[A] = {
    class JsonArrayWrapStage extends GraphStage[FlowShape[Array[Byte], Array[Byte]]] {
      private val in                                          = Inlet[Array[Byte]]("JsonArrayWrapStage.in")
      private val out                                         = Outlet[Array[Byte]]("JsonArrayWrapStage.out")
      override val shape: FlowShape[Array[Byte], Array[Byte]] = FlowShape(in, out)

      private var eventNum       = 1
      private var dataWasWritten = false

      private def makeEventExtraBytes() =
        if (singleValue) {
          Array.empty[Byte]
        } else if (eventNum == 1) {
          eventNum += 1
          "[\r\n".getBytes
        } else if (eventNum > 1) {
          ",".getBytes
        } else {
          eventNum += 1
          Array.empty[Byte]
        }

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
        new GraphStageLogic(shape) with InHandler with OutHandler {
          override def onPush(): Unit = {
            val toPrepend = makeEventExtraBytes()
            push(out, toPrepend ++ grab(in))
            dataWasWritten = true
          }

          override def onPull(): Unit = pull(in)

          def tryWriteArrayFinish(): Unit = {
            if (!singleValue && dataWasWritten) try {
              push(out, "\r\n]".getBytes)
            } catch {
              case _: Throwable =>
            }
          }

          override def onUpstreamFinish(): Unit = {
            tryWriteArrayFinish()
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            tryWriteArrayFinish()
            super.onUpstreamFailure(ex)
          }

          setHandlers(in, out, this)
        }
    }

    Sink {
      StreamFlow[A]
        .map(implicitly[Codec[A]].encode(_))
        .via(new JsonArrayWrapStage)
        .map(ByteString.apply)
        .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)
    }
  }

//  def sqlTable[A](): Sink[A] = ???

  def ignore[A]: Sink[A] = Sink(StreamSink.ignore)

  def console[A]: Sink[A] = Sink {
    StreamSink.foreach[A] { x =>
      println(x)
    }
  }
}
