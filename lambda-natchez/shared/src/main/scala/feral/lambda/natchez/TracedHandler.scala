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

package feral.lambda.natchez

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import feral.lambda.Invocation
import natchez.EntryPoint
import natchez.Span
import natchez.Trace

object TracedHandler {

  def apply[Event, Result](entryPoint: EntryPoint[IO])(
      handler: Trace[IO] => IO[Option[Result]])(
      implicit inv: Invocation[IO, Event],
      KS: KernelSource[Event]): IO[Option[Result]] = for {
    event <- inv.event
    context <- inv.context
    kernel = KS.extract(event)
    result <- entryPoint.continueOrElseRoot(context.functionName, kernel).use { span =>
      span.put(
        AwsTags.arn(context.invokedFunctionArn),
        AwsTags.requestId(context.awsRequestId)
      ) >> Trace.ioTrace(span) >>= handler
    }
  } yield result

  def apply[F[_]: MonadCancelThrow, Event, Result](
      entryPoint: EntryPoint[F],
      handler: Kleisli[F, Span[F], Option[Result]])(
      // inv first helps bind Event for KernelSource. h/t @bpholt
      implicit inv: Invocation[F, Event],
      KS: KernelSource[Event]): F[Option[Result]] = for {
    event <- inv.event
    context <- inv.context
    kernel = KS.extract(event)
    result <- entryPoint.continueOrElseRoot(context.functionName, kernel).use { span =>
      span.put(
        AwsTags.arn(context.invokedFunctionArn),
        AwsTags.requestId(context.awsRequestId)
      ) >> handler(span)
    }
  } yield result

}
