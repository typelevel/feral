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
import cats.effect.kernel.Resource
import io.circe.Encoder

class LambdaSuite {

  // Test for Nothing type inference

  def lambda[F[_]]: Lambda[F, Unit, Nothing] = ???

  def middleware[F[_], Event, Result](lambda: Lambda[Kleisli[F, Unit, *], Event, Result])
      : Resource[F, Lambda[F, Event, Result]] = ???

  implicit def nothingEncoder: Encoder[Nothing] = ???

  class NothingLambda extends IOLambda[Unit, Nothing] {
    def handler = middleware /*[IO, Unit, Nothing]*/ (lambda[Kleisli[IO, Unit, *]])
  }

  // end test

}
