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

package feral.examples

import cats.effect._
import cats.effect.std.Random
import feral.lambda._
import feral.lambda.events.KinesisStreamEvent
import natchez.Trace
import natchez.xray.XRay
import skunk.Session

/**
 * On Scala.js, implement your Lambda as an `object`. This will be the name your JavaScript
 * function is exported as. On JVM, implement your Lambda as a `class`.
 *
 * Every Lambda is triggered by an `Event` for which there must be a circe `Decoder[Event]`. It
 * should then return `Some[Result]` for which there must be a circe `Encoder[Result]`. If your
 * Lambda has no result (as is often the case), use `INothing` and return `None` in the handler.
 *
 * Models for events/results are provided in the `feral.lambda.events` package. There are many
 * more to implement! Please consider contributing to
 * [[https://github.com/typelevel/feral/issues/48 #48]].
 *
 * For a more advanced example, see the `Http4sLambda` next.
 */
object kinesisHandler extends IOLambda.Simple[KinesisStreamEvent, INothing] {

  /**
   * Optional initialization section. This is a resource that will be acquired exactly once when
   * your lambda starts and re-used for each event that it processes.
   *
   * If you do not need to perform any such initialization, you may omit this section.
   */
  type Init = Session[IO] // a skunk session
  override def init =
    Resource.eval(Random.scalaUtilRandom[IO]).flatMap { implicit random =>
      XRay.entryPoint[IO]().flatMap { entrypoint =>
        entrypoint.root("root").evalMap(Trace.ioTrace).flatMap { implicit trace =>
          Session.single[IO](host = "host", user = "user", database = "db")
        }
      }
    }

  /**
   * This is where you implement the logic of your handler.
   *
   * @param event
   *   that triggered your lambda
   * @param context
   *   provides information about the invocation, function, and execution environment
   * @param init
   *   in this example, the skunk session we setup above
   */
  def apply(event: KinesisStreamEvent, context: Context[IO], init: Init) =
    IO.println(s"Received event with \${event.records.size} records").as(None)

}
