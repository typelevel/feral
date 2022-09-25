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
package runtime

import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import io.circe.Json
import org.http4s.client.Client

object FeralLambdaRuntime {

  def apply[F[_]](client: Client[F])(handler: (Json, Context[F]) => F[Json])(
      implicit F: Concurrent[F]
  ): Resource[F, Unit] =
    // TODO implement a runtime here
    // it should retrieve incoming events and handle them with the handler
    // it will run on a background fiber, whose lifecycle is controlled by the resource
    ???

}
