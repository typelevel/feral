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

import cats.effect.IO
import cats.effect.IOLocal
import cats.effect.kernel.Resource
import cats.effect.syntax.all._
import io.circe.Decoder
import io.circe.Encoder

abstract class IOLambda[Event, Result](
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result]
) extends IOLambdaPlatform[Event, Result]
    with IOSetup {

  final type Setup = (Event, Context[IO]) => IO[Option[Result]]
  final override protected def setup: Resource[IO, Setup] = for {
    handler <- handler
    localEvent <- IOLocal[Event](null.asInstanceOf[Event]).toResource
    localContext <- IOLocal[Context[IO]](null).toResource
    env = LambdaEnv.ioLambdaEnv(localEvent, localContext)
    result = handler(env)
  } yield { localEvent.set(_) *> localContext.set(_) *> result }

  def handler: Resource[IO, LambdaEnv[IO, Event] => IO[Option[Result]]]

}

object IOLambda {

  abstract class Simple[Event, Result](
      implicit decoder: Decoder[Event],
      encoder: Encoder[Result])
      extends IOLambda[Event, Result] {

    type Init
    def init: Resource[IO, Init] = Resource.pure(null.asInstanceOf[Init])

    final def handler = init.map { init => env =>
      for {
        event <- env.event
        ctx <- env.context
        result <- apply(event, ctx, init)
      } yield result
    }

    def apply(event: Event, context: Context[IO], init: Init): IO[Option[Result]]
  }

}
