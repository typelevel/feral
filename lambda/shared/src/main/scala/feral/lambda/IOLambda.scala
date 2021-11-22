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
import cats.effect.kernel.Resource
import io.circe.Decoder
import io.circe.Encoder
import cats.effect.IOLocal

abstract class IOLambda[Event, Result](
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result]
) extends IOLambdaPlatform[Event, Result]
    with IOSetup {

  final type Setup = (Event, Context[IO]) => IO[Option[Result]]
  final override protected def setup: Resource[IO, Setup] =
    Resource
      .eval((IOLocal(null.asInstanceOf[Event]).product(IOLocal[Context[IO]](null))))
      .flatMap {
        case (localEvent, localContext) =>
          handler(LambdaEnv.ioLambdaEnv(localEvent, localContext)).map {
            result => (event, context) =>
              localEvent.set(event) *> localContext.set(context) *> result
          }
      }

  def handler(implicit env: LambdaEnv[IO, Event]): Resource[IO, IO[Option[Result]]]

}
