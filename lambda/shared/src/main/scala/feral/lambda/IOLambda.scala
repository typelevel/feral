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

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import io.circe.Decoder
import io.circe.Encoder

abstract class IOLambda[Setup, Event, Result](
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result]
) extends IOLambdaPlatform[Setup, Event, Result] {

  protected def runtime: IORuntime = IORuntime.global

  def setup: Resource[IO, Setup]

  private[lambda] final val setupMemo: IO[Setup] = {
    val deferred = Deferred.unsafe[IO, Either[Throwable, Setup]]
    setup
      .attempt
      .allocated
      .flatTap {
        case (setup, _) =>
          deferred.complete(setup)
      }
      .unsafeRunAndForget()(runtime)
    deferred.get.rethrow
  }

  def apply(event: Event, context: Context, setup: Setup): IO[Option[Result]]

}
