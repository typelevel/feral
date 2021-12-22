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

package feral.netlify

import cats.effect.IO
import cats.effect.kernel.Resource
import feral.lambda
import feral.lambda.IOLambda

import scala.scalajs.js

abstract class IOFunction extends IOLambda[HttpFunctionEvent, HttpFunctionResult] {

  override def main(args: Array[String]): Unit = {
    // Netlify functions require the entrypoint to be called `handler`
    js.Dynamic.global.exports.updateDynamic("handler")(handlerFn)
  }

}

object IOFunction {

  abstract class Simple extends IOFunction {

    type Init
    def init: Resource[IO, Init] = Resource.pure(null.asInstanceOf[Init])

    final def handler = init.map { init => env =>
      for {
        event <- env.event
        ctx <- env.context
        result <- handle(event, ctx, init)
      } yield result
    }

    def handle(
        event: HttpFunctionEvent,
        context: lambda.Context[IO],
        init: Init): IO[Option[HttpFunctionResult]]
  }

}
