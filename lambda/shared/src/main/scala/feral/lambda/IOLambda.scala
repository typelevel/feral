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

import scala.annotation.nowarn

abstract class IOLambda[Event, Result](
    implicit private[lambda] val decoder: Decoder[Event],
    private[lambda] val encoder: Encoder[Result]
) extends IOLambdaPlatform[Event, Result]
    with IOSetup {

  final type Setup = Lambda[IO, Event, Result]

  final override protected def setup: Resource[IO, Setup] = handler

  def handler: Resource[IO, Lambda[IO, Event, Result]]

}

object IOLambda {

  /**
   * This can't actually be used. It's here because `IOLambda` demands an Encoder for its result
   * type, which should be `Nothing` when no output is desired. Userland code will return an
   * `Option[Nothing]` which is only inhabited by `None`, and the encoder is only used when the
   * userland code returns `Some`.
   */
  @nowarn("msg=dead code following this construct")
  implicit val nothingEncoder: Encoder[INothing] = (_: INothing) => ???
}
