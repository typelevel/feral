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

// TODO remove and replace with version in Natchez
package feral.lambda.tracing

import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import cats.~>
import natchez._

trait LiftTrace[F[_], G[_]] {
  def run[A](span: Span[F])(f: Trace[G] => G[A]): F[A]
  def liftK: F ~> G
}

object LiftTrace extends LiftTraceLowPriority {
  def apply[F[_], G[_]](implicit LT: LiftTrace[F, G]): LiftTrace[F, G] = LT

  implicit val ioInstance: LiftTrace[IO, IO] = new LiftTrace[IO, IO] {
    def run[A](span: Span[IO])(f: Trace[IO] => IO[A]): IO[A] =
      Trace.ioTrace(span).flatMap(f)
    def liftK: IO ~> IO = FunctionK.id
  }
}

private[tracing] trait LiftTraceLowPriority {
  implicit def kleisliInstance[F[_]: MonadCancelThrow]: LiftTrace[F, Kleisli[F, Span[F], *]] =
    new LiftTrace[F, Kleisli[F, Span[F], *]] {
      def run[A](span: Span[F])(f: Trace[Kleisli[F, Span[F], *]] => Kleisli[F, Span[F], A]): F[A] =
        f(Trace.kleisliInstance).run(span)
      def liftK: F ~> Kleisli[F, Span[F], *] = Kleisli.liftK
    }
}
