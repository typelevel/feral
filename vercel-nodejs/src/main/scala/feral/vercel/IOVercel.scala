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

package feral.vercel

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import org.http4s.HttpApp
import org.http4s.nodejs.IncomingMessage
import org.http4s.nodejs.ServerResponse

import scala.scalajs.js

abstract class IOVercel {

  final def main(args: Array[String]): Unit =
    js.Dynamic.global.module.exports = handlerFn

  protected def runtime: IORuntime = IORuntime.global

  def handler: Resource[IO, HttpApp[IO]]

  private[vercel] lazy val handlerFn: js.Function2[IncomingMessage, ServerResponse, Unit] = {
    val dispatcherHandle = {
      Dispatcher
        .parallel[IO](await = false)
        .product(handler)
        .allocated
        .map(_._1) // drop unused finalizer
        .unsafeToPromise()(runtime)
    }

    (request: IncomingMessage, response: ServerResponse) =>
      val _ = dispatcherHandle.`then`[Unit] {
        case (dispatcher, handle) =>
          dispatcher.unsafeRunAndForget(
            request.toRequest[IO].flatMap(handle(_)).flatMap(response.writeResponse[IO])
          )
      }
  }
}