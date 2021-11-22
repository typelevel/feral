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

import cats.Functor
import cats.effect.IO
import cats.effect.IOLocal
import cats.syntax.all._
import cats.~>

trait LambdaEnv[F[_], Event] { outer =>
  def event: F[Event]
  def context: F[Context[F]]

  final def mapK[G[_]: Functor](f: F ~> G): LambdaEnv[G, Event] =
    new LambdaEnv[G, Event] {
      def event = f(outer.event)
      def context = f(outer.context).map(_.mapK(f))
    }
}

object LambdaEnv {
  private[lambda] def ioLambdaEnv[Event](
      localEvent: IOLocal[Event],
      localContext: IOLocal[Context[IO]]): LambdaEnv[IO, Event] =
    new LambdaEnv[IO, Event] {
      def event = localEvent.get
      def context = localContext.get
    }
}
