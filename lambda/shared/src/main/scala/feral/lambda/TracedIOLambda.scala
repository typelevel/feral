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

package feral
package lambda

import cats.effect.kernel.Resource
import cats.effect.syntax.all._
import cats.effect.{IO, IOLocal}
import io.circe.{Decoder, Encoder}
import natchez._

import java.net.URI

abstract class TracedIOLambda[Event, Result](entryPoint: IO[EntryPoint[IO]])(
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result],
    private[lambda] val KS: KernelSource[Event]
) extends IOLambdaPlatform[Event, Result]
    with IOLambdaSetup[Event, Result]
    with IOSetup[(Event, Context[IO])] {

  final override protected def setup(ctx: (Event, Context[IO])): Resource[IO, Setup] = for {
    localEvent <- IOLocal(ctx._1).toResource
    localContext <- IOLocal(ctx._2).toResource
    env = LambdaEnv.ioLambdaEnv(localEvent, localContext)
    trace <- Trace.ioTrace(defaultSpan(env)).toResource
    setupResult = IOSetup(handler.tupled)(env -> trace)
  } yield {
    localEvent.set(_) *> localContext.set(_) *> trace.span("")(setupResult.flatten)
  }

  final override def setupAndRun(ev: Event, ctx: Context[IO]): IO[Option[Result]] =
    setupMemo(ev -> ctx).flatMap(_(ev, ctx))

  private[this] def defaultSpan(env: LambdaEnv[IO, Event]): Span[IO] =
    new Span[IO] {
      def put(fields: (String, TraceValue)*): IO[Unit] = IO.unit

      def kernel: IO[Kernel] = env.event.map(KS.extract)

      def span(name: String): Resource[IO, Span[IO]] = for {
        ctx <- env.context.toResource
        k <- kernel.toResource
        ep <- entryPoint.toResource
        span <- ep.continueOrElseRoot(ctx.functionName, k).evalTap { span =>
          span.put(
            AwsTags.arn(ctx.invokedFunctionArn),
            AwsTags.requestId(ctx.awsRequestId)
          )
        }
      } yield span

      def traceId: IO[Option[String]] = IO.none

      def spanId: IO[Option[String]] = IO.none

      def traceUri: IO[Option[URI]] = IO.none
    }

  def handler: (LambdaEnv[IO, Event], Trace[IO]) => Resource[IO, IO[Option[Result]]]
}
