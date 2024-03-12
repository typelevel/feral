// /*
//  * Copyright 2021 Typelevel
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package feral.examples

// import cats.Monad
// import cats.effect.IO
// import cats.syntax.all._
// import feral.lambda.INothing
// import feral.lambda.IOLambda
// import feral.lambda.Invocation
// import feral.lambda.events.SqsEvent
// import feral.lambda.events.SqsRecord
// import feral.lambda.otel4s.SqsRecordTraceAttributes
// import feral.lambda.otel4s.TracedHandler
// import feral.lambda.otel4s._
// import org.http4s.client.Client
// import org.http4s.ember.client.EmberClientBuilder
// import org.typelevel.otel4s.sdk.OpenTelemetrySdk
// import org.typelevel.otel4s.trace.Tracer
// import org.typelevel.scalaccompat.annotation.unused

// object SqsOtelExample extends IOLambda[SqsEvent, INothing] {

//   def handler = {
//     OpenTelemetrySdk
//       .autoConfigured[IO]()
//       .map(_.sdk)
//       .evalMap(_.tracerProvider.get("feral.examples.SqsOtelExample"))
//       .flatMap { implicit tracer: Tracer[IO] =>
//         for {
//           client <- EmberClientBuilder.default[IO].build
//           tracedClient = clientTraceMiddleware(client)
//         } yield { implicit inv: Invocation[IO, SqsEvent] =>
//           TracedHandler[IO, SqsEvent, INothing](
//             handleEvent[IO](tracedClient)
//           )
//         }
//       }
//   }

//   def handleEvent[F[_]: Monad: Tracer](
//       @unused client: Client[F]
//   ): SqsEvent => F[Option[INothing]] = { event =>
//     event
//       .records
//       .traverse(record =>
//         Tracer[F].span("handle-record", SqsRecordTraceAttributes(record)).surround {
//           handleRecord[F](record)
//         })
//       .as(None)
//   }

//   def handleRecord[F[_]: Monad: Tracer](@unused record: SqsRecord): F[Unit] = {
//     Tracer[F].span("some-operation").surround {
//       Monad[F].unit
//     }
//   }

//   // stub client middleware while http4s-otel-middleware catches up to otel4s
//   // 0.5
//   private def clientTraceMiddleware[F[_]](client: Client[F])(implicit @unused T: Tracer[IO]) = {
//     client
//   }

// }
