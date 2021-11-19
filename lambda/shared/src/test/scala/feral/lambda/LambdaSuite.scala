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

class LambdaSuite {

  // Test for INothing type inference

  def lambda[F[_]]: Lambda[F, Unit, INothing] = ???

  def middleware[F[_], Event, Result](lambda: Lambda[Kleisli[F, Unit, *], Event, Result])
      : Resource[F, Lambda[F, Event, Result]] = ???

  class NothingLambda extends IOLambda[Unit, INothing] {
    def handler = middleware(lambda[Kleisli[IO, Unit, *]])
  }

  // end test

}
