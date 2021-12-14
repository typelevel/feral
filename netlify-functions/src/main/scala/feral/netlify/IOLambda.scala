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
import cats.effect.IOLocal
import cats.effect.kernel.Resource
import feral.IOSetup
import io.circe.Decoder
import io.circe.Encoder
import io.circe.scalajs._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

abstract class IOLambda[Event, Result](
    implicit private[netlify] val decoder: Decoder[Event],
    private[netlify] val encoder: Encoder[Result]
) extends IOSetup {

  final type Setup = (Event, Context[IO]) => IO[Option[Result]]

  final override protected def setup: Resource[IO, Setup] =
    handler.map { handler => (event, context) =>
      for {
        event <- IOLocal(event)
        context <- IOLocal(context)
        env = LambdaEnv.ioLambdaEnv(event, context)
        result <- handler(env)
      } yield result
    }

  def main(args: Array[String]): Unit = {
    val handlerName = getClass.getSimpleName.init
    js.Dynamic.global.exports.updateDynamic(handlerName)(handlerFn)
  }

  private lazy val handlerFn
      : js.Function2[js.Any, facade.Context, js.Promise[js.Any | Unit]] = {
    (event: js.Any, context: facade.Context) =>
      (for {
        lambda <- setupMemo
        event <- IO.fromEither(decodeJs[Event](event))
        result <- lambda(event, Context.fromJS(context))
      } yield result.map(_.asJsAny).orUndefined).unsafeToPromise()(runtime)
  }

  def handler: Resource[IO, LambdaEnv[IO, Event] => IO[Option[Result]]]

}

object IOLambda {

  abstract class Simple[Event, Result](
      implicit decoder: Decoder[Event],
      encoder: Encoder[Result])
      extends IOLambda[Event, Result] {

    type Init
    def init: Resource[IO, Init] = Resource.pure(null.asInstanceOf[Init])

    final def handler = init.map { init => env =>
      for {
        event <- env.event
        ctx <- env.context
        result <- handle(event, ctx, init)
      } yield result
    }

    def handle(event: Event, context: Context[IO], init: Init): IO[Option[Result]]
  }

}
