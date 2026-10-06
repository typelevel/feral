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

package feral.googlecloud

import cats.effect.Async
import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Dispatcher
import cats.effect.syntax.all._
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import com.google.cloud.functions.CloudEventsFunction
import io.circe.Decoder
import io.cloudevents.CloudEvent

import scala.util.control.NonFatal

abstract class IOCloudEventsFunction[Event, Result](
    implicit private[googlecloud] val decoder: Decoder[Event]
) extends CloudEventsFunction {

  // IOLambda equivalent
  protected def runtime: IORuntime = IORuntime.global

  def handler: Resource[IO, ContextEventWithData[Event] => IO[Unit]]

  // IOLambdaPlatform equivalent
  private[this] val (dipatcher, handle) = {
    val handler = {
      val h =
        try this.handler
        catch { case ex if NonFatal(ex) => null }

      if (h ne null) {
        h.map(IO.pure(_))
      } else {
        val functionName = getClass().getSimpleName()
        val msg =
          s"""|There was an error initializing `$functionName` during startup.
              |Falling back to initialize-during-first-invocation strategy.
              |To fix, try replacing any `val`s in `$functionName` with `def`s.""".stripMargin
        System.err.println(msg)

        Async[Resource[IO, *]].defer(this.handler).memoize.map(_.allocated.map(_._1))
      }
    }

    Dispatcher
      .parallel[IO](await = false)
      .product(handler)
      .allocated
      .map(_._1)
      .unsafeRunSync()(runtime)
  }

  final def accept(event: CloudEvent): Unit = {
    val cloudEventWithContextData = IO.fromEither(ContextEventWithData.from[Event](event))

    dipatcher.unsafeRunSync(
      handle.flatMap { func => cloudEventWithContextData.flatMap(func) }
    )
  }

}

object IOCloudEventsFunction {
  abstract class Simple[Event, Result](
      implicit decoder: Decoder[Event]
  ) extends IOCloudEventsFunction[Event, Result] {

    type Init
    def init: Resource[IO, Init] = Resource.pure(null.asInstanceOf[Init])

    final def handler = init.map { init => event =>
      for {
        result <- apply(event.data, event.context, init)
      } yield result
    }

    def apply(event: Event, context: CloudEventContext, init: Init): IO[Unit]
  }

}
