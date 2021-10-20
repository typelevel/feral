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

import cats.syntax.all._
import io.circe.scalajs._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

private[lambda] trait LambdaPlatform[F[_], Event, Result] {
  this: Lambda[F, Event, Result] =>

  // @JSExportTopLevel("handler") // TODO
  final def handler(event: js.Any, context: facade.Context): js.Promise[js.Any | Unit] =
    dispatcher.unsafeToPromise {
      for {
        setup <- setupMemo
        event <- decodeJs[Event](event).liftTo
        result <- apply(event, Context.fromJS(context), setup)
      } yield result.map(_.asJsAny).orUndefined
    }
}
