package feral.examples

import cats.syntax.all._
import org.typelevel.otel4s.sdk.OpenTelemetrySdk
import feral.lambda.IOLambda
import cats.effect.IO
import feral.lambda.events.SqsEvent
import feral.lambda.INothing
import feral.lambda.Invocation
import org.typelevel.otel4s.trace.Tracer
import org.http4s.client.Client
import org.typelevel.scalaccompat.annotation.unused
import org.http4s.ember.client.EmberClientBuilder
import cats.Monad
import feral.lambda.events.SqsRecord
import feral.lambda.otel4s.SqsRecordTraceAttributes
import feral.lambda.otel4s.TracedHandler
import feral.lambda.otel4s.implicits._

object SqsOtelExample extends IOLambda[SqsEvent, INothing] {

  def handler = {
    OpenTelemetrySdk
      .autoConfigured[IO]()
      .map(_.sdk)
      .evalMap(_.tracerProvider.get("feral.examples.SqsOtelExample"))
      .flatMap { implicit tracer: Tracer[IO] =>
        for {
          client <- EmberClientBuilder.default[IO].build
          tracedClient = clientTraceMiddleware(client)
        } yield { implicit inv: Invocation[IO, SqsEvent] =>
          TracedHandler[IO, SqsEvent, INothing](
            handleEvent[IO](tracedClient)
          )
        }
      }
  }

  def handleEvent[F[_]: Monad: Tracer](
      @unused client: Client[F]
  ): SqsEvent => F[Option[INothing]] = { event =>
    event
      .records
      .traverse(record =>
        Tracer[F].span("handle-record", SqsRecordTraceAttributes(record)).surround {
          handleRecord[F](record)
        })
      .as(None)
  }

  def handleRecord[F[_]: Monad: Tracer](@unused record: SqsRecord): F[Unit] = {
    Tracer[F].span("some-operation").surround {
      Monad[F].unit
    }
  }

  // stub client middleware while http4s-otel-middleware catches up to otel4s
  // 0.5
  private def clientTraceMiddleware[F[_]](client: Client[F])(implicit @unused T: Tracer[IO]) = {
    client
  }

}
