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

import cats.effect.IO
import cats.data.Kleisli
import cats.data.WriterT
import cats.data.StateT
import cats.data.EitherT
import cats.data.OptionT

// compile-time tests
trait LambdaEnvLiftSuite {
  trait Event

  implicit val env: LambdaEnv[IO, Event]

  implicitly[LambdaEnv[Kleisli[IO, String, *], Event]]
  implicitly[LambdaEnv[WriterT[IO, String, *], Event]]
  implicitly[LambdaEnv[StateT[IO, String, *], Event]]
  implicitly[LambdaEnv[EitherT[IO, String, *], Event]]
  implicitly[LambdaEnv[OptionT[IO, *], Event]]
  // doesn't work with this PR, works without it
  // implicitly[LambdaEnv[Kleisli[EitherT[IO, String, *], String, *], Event]]
}
