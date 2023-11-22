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

sealed trait Invocation[F[_], Event] {
  def event: F[Event]
  def context: F[Context[F]]

  def mapK[G[_]](fk: F ~> G): Invocation[G, Event]
}

object Invocation {
  def apply[F[_], Event](implicit inv: Invocation[F, Event]): Invocation[F, Event] = inv

  def event[F[_], Event](implicit inv: Invocation[F, Event]): F[Event] = inv.event
  def context[F[_], Event](implicit inv: Invocation[F, Event]): F[Context[F]] = inv.context

  def pure[F[_]: Applicative, Event](e: Event, c: Context[F]): Invocation[F, Event] =
    new Invocation[F, Event] {
      def event = e.pure[F]
      def context = c.pure[F]
      def mapK[G[_]](fk: F ~> G) = new MapK(this, fk)
    }

  implicit def kleisliInvocation[F[_], A, B](
      implicit inv: Invocation[F, A]): Invocation[Kleisli[F, B, *], A] =
    inv.mapK(Kleisli.liftK)

  implicit def optionTInvocation[F[_]: Functor, A](
      implicit inv: Invocation[F, A]): Invocation[OptionT[F, *], A] =
    inv.mapK(OptionT.liftK)

  implicit def eitherTInvocation[F[_]: Functor, A, B](
      implicit inv: Invocation[F, A]): Invocation[EitherT[F, B, *], A] =
    inv.mapK(EitherT.liftK)

  implicit def writerTInvocation[F[_]: Applicative, A, B: Monoid](
      implicit inv: Invocation[F, A]): Invocation[WriterT[F, B, *], A] =
    inv.mapK(WriterT.liftK[F, B])

  implicit def stateTInvocation[F[_]: Applicative, S, A](
      implicit inv: Invocation[F, A]): Invocation[StateT[F, S, *], A] =
    inv.mapK(StateT.liftK[F, S])

  private[lambda] def ioInvocation[Event](
      localEvent: IOLocal[Event],
      localContext: IOLocal[Context[IO]]): Invocation[IO, Event] =
    new Invocation[IO, Event] {
      def event = localEvent.get
      def context = localContext.get
      def mapK[F[_]](fk: IO ~> F) = new MapK(this, fk)
    }

  private final class MapK[F[_]: Functor, G[_], Event](
      underlying: Invocation[F, Event],
      fk: F ~> G
  ) extends Invocation[G, Event] {
    def event = fk(underlying.event)
    def context = fk(underlying.context.map(_.mapK(fk)))
    def mapK[H[_]](gk: G ~> H) = new MapK(underlying, fk.andThen(gk))
  }
}
