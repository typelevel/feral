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

package feral.netlify

import cats.Applicative
import cats.Functor
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.data.WriterT
import cats.effect.IO
import cats.effect.IOLocal
import cats.kernel.Monoid
import cats.syntax.all._
import cats.~>

sealed trait FunctionEnv[F[_], Event] { outer =>
  def event: F[Event]
  def context: F[Context[F]]

  final def mapK[G[_]: Functor](f: F ~> G): FunctionEnv[G, Event] =
    new FunctionEnv[G, Event] {
      def event = f(outer.event)
      def context = f(outer.context).map(_.mapK(f))
    }
}

object FunctionEnv {
  def apply[F[_], A](implicit env: FunctionEnv[F, A]): FunctionEnv[F, A] = env

  implicit def kleisliLambdaEnv[F[_]: Functor, A, B](
      implicit env: FunctionEnv[F, A]): FunctionEnv[Kleisli[F, B, *], A] =
    env.mapK(Kleisli.liftK)

  implicit def optionTLambdaEnv[F[_]: Functor, A](
      implicit env: FunctionEnv[F, A]): FunctionEnv[OptionT[F, *], A] =
    env.mapK(OptionT.liftK)

  implicit def eitherTLambdaEnv[F[_]: Functor, A, B](
      implicit env: FunctionEnv[F, A]): FunctionEnv[EitherT[F, B, *], A] =
    env.mapK(EitherT.liftK)

  implicit def writerTLambdaEnv[F[_]: Applicative, A, B: Monoid](
      implicit env: FunctionEnv[F, A]): FunctionEnv[WriterT[F, B, *], A] =
    env.mapK(WriterT.liftK[F, B])

  private[netlify] def ioLambdaEnv[Event](
      localEvent: IOLocal[Event],
      localContext: IOLocal[Context[IO]]): FunctionEnv[IO, Event] =
    new FunctionEnv[IO, Event] {
      def event = localEvent.get
      def context = localContext.get
    }
}
