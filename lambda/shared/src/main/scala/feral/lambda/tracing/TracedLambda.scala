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

package feral.lambda.tracing

import cats._
import cats.data._
import cats.effect.std.Random
import cats.effect.{Trace => _, _}
import natchez._
import feral.lambda._
import natchez.noop.NoopSpan
import natchez.xray.{XRay, XRayEnvironment}

abstract class TracedLambda[F[_]: MonadCancelThrow, G[_], Event] {
  def extractKernel(event: Event, context: Context[F]): F[Kernel]

  def runTraced[A](span: Span[F])(ga: G[A]): F[A]

  def apply[Result](entryPoint: EntryPoint[F], fk: F ~> G)(
      lambda: Lambda[G, Event, Result]): Lambda[F, Event, Result] =
    (event, context) =>
      Resource
        .eval(extractKernel(event, context))
        .flatMap { kernel => entryPoint.continueOrElseRoot(context.functionName, kernel) }
        .use { span => runTraced(span)(lambda(event, context.mapK(fk))) }
}

abstract class KleisliTracedLambda[F[_]: MonadCancelThrow, Event]
    extends TracedLambda[F, Kleisli[F, Span[F], *], Event] {
  override def runTraced[A](span: Span[F])(ga: Kleisli[F, Span[F], A]): F[A] = ga(span)
}

sealed abstract class TracedIO[A] {
  def run(span: Span[IO]): IO[A]
}

object TracedIO {
  def apply[A](f: Trace[IO] => IO[A]): TracedIO[A] =
    new TracedIO[A] {
      def run(span: Span[IO]): IO[A] = Trace.ioTrace(span).flatMap(f)
    }
}

abstract class TracedIOLambda[Event] extends TracedLambda[IO, TracedIO, Event] {
  override def runTraced[A](span: Span[IO])(ga: TracedIO[A]): IO[A] = ga.run(span)
}

class XRayKleisliTracedLambda[F[_]: Sync, Event] extends KleisliTracedLambda[F, Event] {
  override def extractKernel(event: Event, context: Context[F]): F[Kernel] =
    XRayEnvironment[F].kernelFromEnvironment
}

object XRayKleisliTracedLambda {
  def apply[F[_]: Async, Event, Result](
      installer: Resource[
        Kleisli[F, Span[F], *],
        Lambda[Kleisli[F, Span[F], *], Event, Result]]): Resource[F, Lambda[F, Event, Result]] =
    installer
      .mapK(
        Kleisli.applyK(new NoopSpan[F])
      ) // TODO does this work? I think yes, b/c I think it only affects the Resource acquisition, not the actual resource?
      .flatMap(XRayKleisliTracedLambda(_))

  def apply[F[_]: Async, Event, Result](lambda: Lambda[Kleisli[F, Span[F], *], Event, Result])
      : Resource[F, Lambda[F, Event, Result]] = {
    Resource.eval(Random.scalaUtilRandom[F]).flatMap { implicit random =>
      Resource
        .eval(XRayEnvironment[F].daemonAddress)
        .flatMap {
          case Some(addr) => XRay.entryPoint[F](addr)
          case None => XRay.entryPoint[F]()
        }
        .map(new XRayKleisliTracedLambda[F, Event].apply(_, Kleisli.liftK[F, Span[F]])(lambda))
    }
  }
}
