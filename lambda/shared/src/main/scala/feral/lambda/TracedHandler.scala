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

package feral.lambda

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.{Trace => _, _}
import cats.mtl.Local
import cats.syntax.all._
import fs2.compat.NotGiven
import natchez._
import natchez.mtl._

object TracedHandler extends TracedHandlerPlatform {

  def apply[Event, Result](
      entryPoint: EntryPoint[IO],
      handler: Trace[IO] => IO[Option[Result]])(
      implicit inv: Invocation[IO, Event],
      KS: KernelSource[Event],
      @annotation.unused NotLocal: NotGiven[Local[IO, Span[IO]]]): IO[Option[Result]] =
    for {
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
      implicit inv: Invocation[F, Event],
      KS: KernelSource[Event],
      @annotation.unused NotLocal: NotGiven[Local[IO, Span[IO]]]): F[Option[Result]] =
    for {
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

  @deprecated("use variant with Local tracing semantics", "0.3.2")
  def apply[Event, Result](
      entryPoint: EntryPoint[IO],
      handler: Trace[IO] => IO[Option[Result]],
      inv: Invocation[IO, Event],
      KS: KernelSource[Event]): IO[Option[Result]] =
    apply(entryPoint, handler)(inv, KS, implicitly)

  @deprecated("use variant with Local tracing semantics", "0.3.2")
  def apply[F[_], Event, Result](
      entryPoint: EntryPoint[F],
      handler: Kleisli[F, Span[F], Option[Result]],
      M: MonadCancel[F, Throwable],
      inv: Invocation[F, Event],
      KS: KernelSource[Event]): F[Option[Result]] = {
    apply(entryPoint, handler)(M, inv, KS, implicitly)
  }

}

private[lambda] object TracedHandlerImpl {
  def apply[F[_]: MonadCancelThrow, Event, Result](entryPoint: EntryPoint[F])(
      handler: Trace[F] => F[Option[Result]])(
      implicit inv: Invocation[F, Event],
      KS: KernelSource[Event],
      L: Local[F, Span[F]]): F[Option[Result]] =
    for {
      event <- Invocation[F, Event].event
      context <- Invocation[F, Event].context
      kernel = KernelSource[Event].extract(event)
      result <- entryPoint.continueOrElseRoot(context.functionName, kernel).use {
        Local[F, Span[F]].scope {
          Trace[F].put(
            AwsTags.arn(context.invokedFunctionArn),
            AwsTags.requestId(context.awsRequestId)
          ) >> handler(Trace[F])
        }
      }
    } yield result

}
