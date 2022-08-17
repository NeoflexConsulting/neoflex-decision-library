package ru.neoflex.ndk.testkit.func.source

import akka.NotUsed
import akka.stream.scaladsl.{GraphDSL, Sink, Source, SourceQueueWithComplete}
import akka.stream.{OverflowStrategy, QueueOfferResult, SourceShape, StreamDetachedException}
import cats.effect.kernel.Resource.ExitCase
import cats.effect.unsafe.IORuntime
import cats.effect.{Async, Concurrent, IO}
import cats.implicits._
import fs2.Stream

object Fs2StreamAdapter {
  private def publisherStream[F[_]: Concurrent: Async, A](
    publisher: SourceQueueWithComplete[A],
    stream: Stream[F, A]
  ): Stream[F, Unit] = {
    def publish(a: A) =
      Async[F]
        .fromFuture(Concurrent[F].delay(publisher.offer(a)))
        .flatMap {
          case QueueOfferResult.Enqueued       => ().some.pure[F]
          case QueueOfferResult.Failure(cause) => Concurrent[F].raiseError[Option[Unit]](cause)
          case QueueOfferResult.QueueClosed    => none[Unit].pure[F]
          case QueueOfferResult.Dropped =>
            Concurrent[F].raiseError[Option[Unit]](
              new IllegalStateException("This should never happen because we use OverflowStrategy.backpressure")
            )
        }
        .recover {
          // This handles a race condition between `interruptWhen` and `publish`.
          // There's no guarantee that, when the akka sink is terminated, we will observe the
          // `interruptWhen` termination before calling publish one last time.
          // Such a call fails with StreamDetachedException
          case _: StreamDetachedException => none[Unit]
        }

    def watchCompletion: F[Unit]    = Async[F].fromFuture(Concurrent[F].delay(publisher.watchCompletion())).void
    def fail(e: Throwable): F[Unit] = Concurrent[F].delay(publisher.fail(e)) >> watchCompletion
    def complete: F[Unit]           = Concurrent[F].delay(publisher.complete()) >> watchCompletion

    stream.interruptWhen(watchCompletion.attempt).evalMap(publish).unNoneTerminate.onFinalizeCase {
      case ExitCase.Succeeded | ExitCase.Canceled => complete
      case ExitCase.Errored(e)                    => fail(e)
    }
  }

  def streamToSource[A](stream: Stream[IO, A])(implicit ioRuntime: IORuntime): Source[A, _] = {
    val sink = Sink.foreach[SourceQueueWithComplete[A]] { queue =>
      publisherStream[IO, A](queue, stream).compile.drain.unsafeRunAndForget()
      ()
    }
    val source = Source.queue[A](0, OverflowStrategy.backpressure)
    Source
      .fromGraph(GraphDSL.createGraph(source) { implicit builder => source =>
        import GraphDSL.Implicits._
        builder.materializedValue ~> sink
        SourceShape(source.out)
      })
      .mapMaterializedValue(_ => NotUsed)
  }
}
