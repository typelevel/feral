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
import feral.lambda.LambdaEnv
import natchez.EntryPoint
import natchez.Span
import natchez.Trace

object TracedLambda {

  def apply[F[_]: MonadCancelThrow, Event, Result](entryPoint: EntryPoint[F])(
      lambda: Kleisli[F, Span[F], Option[Result]])(
      // env first helps bind Event for KernelSource. h/t @bpholt
      implicit env: LambdaEnv[F, Event],
      KS: KernelSource[Event]): F[Option[Result]] = for {
    event <- env.event
    context <- env.context
    kernel = KS.extract(event)
    result <- entryPoint.continueOrElseRoot(context.functionName, kernel).use { span =>
      span.put(
        AwsTags.arn(context.invokedFunctionArn),
        AwsTags.requestId(context.awsRequestId)
      ) >> lambda(span)
    }
  } yield result

  def apply[Event, Result](entryPoint: EntryPoint[IO])(lambda: Trace[IO] => IO[Option[Result]])(
      implicit env: LambdaEnv[IO, Event],
      KS: KernelSource[Event]): IO[Option[Result]] = for {
    event <- env.event
    context <- env.context
    kernel = KS.extract(event)
    result <- entryPoint.continueOrElseRoot(context.functionName, kernel).use { span =>
      span.put(
        AwsTags.arn(context.invokedFunctionArn),
        AwsTags.requestId(context.awsRequestId)
      ) >> Trace.ioTrace(span) >>= lambda
    }
  } yield result

}
