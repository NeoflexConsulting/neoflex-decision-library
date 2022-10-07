package ru.neoflex.ndk.strategy.tracking

import cats.Monad
import cats.implicits.catsSyntaxApplicativeId
import io.circe.generic.auto._
import io.circe.{ Encoder, Printer }
import ru.neoflex.ndk.engine.tracking.{ FlowTracker, FlowTrackingObserver, OperatorTrackedEventRoot }

import java.nio.file.{ Files, Paths }
import scala.util.Using

object JsonFileFlowTracker {
  def apply[F[_]](flowTracker: FlowTracker, filename: String)(implicit monadError: Monad[F]): FlowTrackingObserver[F] =
    new FlowTrackingObserver[F](flowTracker, saveEvent[F](filename))

  def saveEvent[F[_]](fileName: String)(event: OperatorTrackedEventRoot)(implicit monad: Monad[F]): F[Unit] = {
    val currentTime = System.currentTimeMillis()
    val targetFilename = s"$fileName-$currentTime.json"
    Using(Files.newBufferedWriter(Paths.get(targetFilename))) { w =>
      val json = Encoder[OperatorTrackedEventRoot].apply(event).printWith(Printer.spaces2)
      w.write(json)
      w.newLine()
    }
    ().pure
  }
}
