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

import cats.data.Kleisli
import cats.effect.IO
import feral.lambda.events.KinesisStreamEvent
import natchez.EntryPoint
import natchez.Span
import natchez.Trace

import scala.annotation.nowarn

class TracedLambdaSuite {

  @nowarn
  def syntaxTest = { // Checking for compilation, nothing more

    implicit def env: LambdaEnv[IO, KinesisStreamEvent] = ???
    def ioEntryPoint: EntryPoint[IO] = ???
    def needsTrace[F[_]: Trace]: F[Option[INothing]] = ???

    TracedHandler(ioEntryPoint) { implicit trace => needsTrace[IO] }

    TracedHandler(ioEntryPoint, Kleisli[IO, Span[IO], Option[INothing]](???))
  }

}
