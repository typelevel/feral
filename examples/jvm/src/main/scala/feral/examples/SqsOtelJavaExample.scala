/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feral.examples

import org.http4s.otel4s.middleware.ClientMiddleware
import _root_.feral.lambda.INothing
import _root_.feral.lambda.IOLambda
import _root_.feral.lambda.Invocation
import _root_.feral.lambda.events.SqsEvent
import _root_.feral.lambda.events.SqsRecord
import _root_.feral.lambda.otel4s.SqsRecordAttributes
import _root_.feral.lambda.otel4s.TracedHandler
import _root_.feral.lambda.otel4s._
import cats.Monad
import cats.effect.IO
import cats.syntax.all._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.scalaccompat.annotation.unused

object SqsOtelExample extends IOLambda[SqsEvent, INothing] {

  def handler =
    OtelJava.autoConfigured[IO]().map(_.tracerProvider).evalMap(_.get("tracer")).flatMap {
      implicit tracer: Tracer[IO] =>
        for {
          client <- EmberClientBuilder.default[IO].build
          tracedClient = ClientMiddleware.default[IO].build(client)
        } yield { implicit inv: Invocation[IO, SqsEvent] =>
          TracedHandler[IO, SqsEvent, INothing](
            handleEvent[IO](tracedClient)
          )
        }
    }

  def handleEvent[F[_]: Monad: Tracer](
      @unused client: Client[F]
  ): SqsEvent => F[Option[INothing]] = { event =>
    event
      .records
      .traverse(record =>
        Tracer[F].span("handle-record", SqsRecordAttributes(record)).surround {
          handleRecord[F](record)
        })
      .as(None)
  }

  def handleRecord[F[_]: Monad: Tracer](@unused record: SqsRecord): F[Unit] = {
    Tracer[F].span("some-operation").surround {
      Monad[F].unit
    }
  }

}
