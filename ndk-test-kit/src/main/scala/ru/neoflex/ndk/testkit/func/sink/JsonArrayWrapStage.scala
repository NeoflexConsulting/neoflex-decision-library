package ru.neoflex.ndk.testkit.func.sink

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

private[func] class JsonArrayWrapStage(singleValue: Boolean) extends GraphStage[FlowShape[Array[Byte], Array[Byte]]] {
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
