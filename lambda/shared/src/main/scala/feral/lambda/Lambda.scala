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
import io.circe.Decoder
import io.circe.Encoder

abstract class Lambda[F[_], Event, Result](
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result]
) extends LambdaPlatform[F, Event, Result]
    with Feral[F] {

  def apply(event: Event, context: Context[F], setup: Setup): F[Option[Result]]
}

trait IOLambda[Event, Result] extends Lambda[IO, Event, Result] with IOFeral
