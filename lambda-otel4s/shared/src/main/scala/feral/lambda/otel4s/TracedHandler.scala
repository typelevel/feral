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

package feral.lambda.otel4s

import cats.Monad
import cats.syntax.all._
import feral.lambda.Context
import feral.lambda.Invocation
import org.typelevel.otel4s.trace.SpanOps
import org.typelevel.otel4s.trace.Tracer

object TracedHandler {

  def apply[F[_]: Monad: Tracer, Event, Result](
      handler: Event => F[Option[Result]]
  )(
      implicit inv: Invocation[F, Event],
      attr: EventSpanAttributes[Event]
  ): F[Option[Result]] =
    for {
      event <- inv.event
      context <- inv.context
      res <- Tracer[F].joinOrRoot(attr.contextCarrier(event)) {
        buildSpan(event, context).surround {
          handler(event)
        }
      }
    } yield res

  def buildSpan[F[_]: Tracer, Event](event: Event, context: Context[F])(
      implicit attr: EventSpanAttributes[Event]
  ): SpanOps[F] =
    Tracer[F]
      .spanBuilder(context.functionName)
      .addAttributes(LambdaContextTraceAttributes(context))
      .withSpanKind(attr.spanKind)
      .addAttributes(attr.attributes(event))
      .build
}
