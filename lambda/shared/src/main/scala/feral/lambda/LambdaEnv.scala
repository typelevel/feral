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

import cats.Applicative
import cats.Functor
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.data.StateT
import cats.data.WriterT
import cats.effect.IO
import cats.effect.IOLocal
import cats.kernel.Monoid
import cats.syntax.all._
import cats.~>

sealed trait LambdaEnv[F[_], Event] {
  def event: F[Event]
  def context: F[Context[F]]

  def mapK[G[_]](fk: F ~> G): LambdaEnv[G, Event]
}

object LambdaEnv {
  def apply[F[_], A](implicit env: LambdaEnv[F, A]): LambdaEnv[F, A] = env

  def event[F[_], Event](implicit env: LambdaEnv[F, Event]): F[Event] = env.event
  def context[F[_], Event](implicit env: LambdaEnv[F, Event]): F[Context[F]] = env.context

  def pure[F[_]: Applicative, Event](e: Event, c: Context[F]): LambdaEnv[F, Event] =
    new LambdaEnv[F, Event] {
      def event = e.pure[F]
      def context = c.pure[F]
      def mapK[G[_]](fk: F ~> G) = new MapK(this, fk)
    }

  implicit def kleisliLambdaEnv[F[_], A, B](
      implicit env: LambdaEnv[F, A]): LambdaEnv[Kleisli[F, B, *], A] =
    env.mapK(Kleisli.liftK)

  implicit def optionTLambdaEnv[F[_]: Functor, A](
      implicit env: LambdaEnv[F, A]): LambdaEnv[OptionT[F, *], A] =
    env.mapK(OptionT.liftK)

  implicit def eitherTLambdaEnv[F[_]: Functor, A, B](
      implicit env: LambdaEnv[F, A]): LambdaEnv[EitherT[F, B, *], A] =
    env.mapK(EitherT.liftK)

  implicit def writerTLambdaEnv[F[_]: Applicative, A, B: Monoid](
      implicit env: LambdaEnv[F, A]): LambdaEnv[WriterT[F, B, *], A] =
    env.mapK(WriterT.liftK[F, B])

  implicit def stateTLambdaEnv[F[_]: Applicative, S, A](
      implicit env: LambdaEnv[F, A]): LambdaEnv[StateT[F, S, *], A] =
    env.mapK(StateT.liftK[F, S])

  private[lambda] def ioLambdaEnv[Event](
      localEvent: IOLocal[Event],
      localContext: IOLocal[Context[IO]]): LambdaEnv[IO, Event] =
    new LambdaEnv[IO, Event] {
      def event = localEvent.get
      def context = localContext.get
      def mapK[F[_]](fk: IO ~> F) = new MapK(this, fk)
    }

  private final class MapK[F[_]: Functor, G[_], Event](
      underlying: LambdaEnv[F, Event],
      fk: F ~> G
  ) extends LambdaEnv[G, Event] {
    def event = fk(underlying.event)
    def context = fk(underlying.context.map(_.mapK(fk)))
    def mapK[H[_]](gk: G ~> H) = new MapK(underlying, fk.andThen(gk))
  }
}
