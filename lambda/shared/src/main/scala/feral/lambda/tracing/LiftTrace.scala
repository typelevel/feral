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

import cats.data.Kleisli
import cats.effect.IO
import natchez.Span

// TODO remove and replace with version in Natchez
trait LiftTrace[F[_], G[_]] {
  def lift[A](span: Span[F], ga: G[A]): F[A]
}

object LiftTrace {
  def apply[F[_], G[_]](implicit LT: LiftTrace[F, G]): LiftTrace[F, G] = LT

  implicit def kleisliInstance[F[_]]: LiftTrace[F, Kleisli[F, Span[F], *]] =
    new LiftTrace[F, Kleisli[F, Span[F], *]] {
      def lift[A](span: Span[F], ga: Kleisli[F, Span[F], A]): F[A] = ga(span)
    }

  implicit val tracedIOInstance: LiftTrace[IO, TracedIO] = new LiftTrace[IO, TracedIO] {
    override def lift[A](span: Span[IO], ga: TracedIO[A]): IO[A] = ga.run(span)
  }
}
