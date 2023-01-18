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
import io.circe.scalajs._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

private[lambda] trait IOLambdaPlatform[Event, Result] {
  this: IOLambda[Event, Result] =>

  /**
   * Lambda handler. Implement this type with a val and a call to `handlerFn` to export your
   * handler.
   *
   * @example
   *   {{{
   *  @JSExportTopLevel("handler")
   *  val handler: HandlerFn = handlerFn
   *   }}}
   */
  final type HandlerFn = js.Function2[js.Any, facade.Context, js.Promise[js.Any | Unit]]

  final protected lazy val handlerFn
      : js.Function2[js.Any, facade.Context, js.Promise[js.Any | Unit]] = {
    (event: js.Any, context: facade.Context) =>
      (for {
        lambda <- setupMemo
        event <- IO.fromEither(decodeJs[Event](event))
        result <- lambda(event, Context.fromJS(context))
      } yield result.map(_.asJsAny).orUndefined).unsafeToPromise()(runtime)
  }
}
