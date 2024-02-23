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
import cats.data.Kleisli
import cats.syntax.all._
import feral.lambda.Context
import feral.lambda.Invocation
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.resource.attributes.ResourceAttributes
import org.typelevel.otel4s.trace.Tracer

object TracedHandler {

  def apply[F[_]: Monad: Tracer, Event, Result](
      handler: Kleisli[F, Event, Option[Result]]
  )(
      implicit inv: Invocation[F, Event],
      etc: EventTraceContext[Event]
  ): F[Option[Result]] =
    for {
      event <- inv.event
      context <- inv.context
      res <- Tracer[F].joinOrRoot(etc.contextCarrier(event)) {
        val spanR =
          Tracer[F]
            .spanBuilder(context.functionName)
            .withSpanKind(etc.spanKind)
            .addAttributes(staticAttributes)
            .addAttributes(contextAttrs(context))
            .addAttributes(etc.attributes(event))
            .build

        spanR.surround {
          for {
            res <- handler(event)
          } yield res
        }
      }
    } yield res

  private def contextAttrs[F[_]](context: Context[F]): List[Attribute[_]] = {
    import LambdaContextAttributes._

    List(
      CloudResourceId(context.invokedFunctionArn),
      FaasInstance(context.logStreamName),
      FaasMaxMemory(context.memoryLimitInMB.toLong),
      FaasName(context.functionName),
      FaasVersion(context.functionVersion)
    )
  }

  private def staticAttributes: List[Attribute[_]] = {
    import ResourceAttributes._

    List(
      CloudProvider(
        ResourceAttributes.CloudProviderValue.Aws.value
      )
    )
  }
}
