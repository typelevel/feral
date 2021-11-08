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

import cats.effect.{IO, Resource}
import cats.syntax.all._
import io.circe.scalajs._
import natchez.Trace.ioTrace

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

private[lambda] trait IOLambdaPlatform[Event, Result] {
  this: IOLambda[Event, Result] =>

  private def ioHandler(event: js.Any, context: facade.Context): IO[js.Any | Unit] =
    Resource
      .pure(Context.fromJS(context))
      .flatMap { (context: Context) =>
        traceRootSpan(context.functionName).evalMap(ioTrace).evalMap { implicit trace =>
          for {
            setup <- setupMemo
            event <- IO.fromEither(decodeJs[Event](event))
            result <- apply(event, context, setup)
          } yield result.map(_.asJsAny).orUndefined
        }
      }
      .use(_.pure[IO])

  // @JSExportTopLevel("handler") // TODO
  final def handler(event: js.Any, context: facade.Context): js.Promise[js.Any | Unit] =
    ioHandler(event, context).unsafeToPromise()(runtime)
}
