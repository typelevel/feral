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

package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._

import feral.functions.facade.InvocationContext
import feral.functions.facade.JSRequest
import feral.functions.util.AppConfig.buildDefaultConfig
import feral.functions.util.AppConfig
import feral.functions.util.Parser

import org.http4s.HttpApp

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import cats.effect.unsafe.IORuntime
import cats.effect.std.Dispatcher
import feral.functions.facade.Context

abstract class IOAzureHttpFunction {
  protected def handler: Context => Resource[IO, HttpApp[IO]]
  protected def appConfig: AppConfig = buildDefaultConfig(handlerFn)
  protected def qBound: Int = 100

  private[functions] val runtime = IORuntime.global

  final def main(args: Array[String]): Unit =
    IOAzureHttpFunction.App.http(functionName, appConfig.toJS)

  private[functions] val functionName: String =
    getClass.getSimpleName.init

  private[functions] lazy val handlerFn
      : js.Function2[JSRequest, InvocationContext, js.Promise[js.UndefOr[js.Any]]] = {
    val dispatcherHandle = {
      Dispatcher
        .parallel[IO](await = true)
        .product(Resource.pure(handler))
        .allocated
        .map(_._1) // drop unused finalizer, this resource will live for the duration
        .unsafeToPromise()(runtime)
    }

    (requestJS, context) => {
      dispatcherHandle.`then`[js.Any] {
        case (dispatcher, handle) => {
          val io = for {
            request <- Parser.decodeRequest[IO](requestJS)
            response <- handle(Context(context)).use(app => app.run(request))
            respEncoded <- Parser.encodeResponse[IO](response, dispatcher, qBound)
          } yield respEncoded

          dispatcher.unsafeToPromise(io)
        }
      }
    }
  }
}

object IOAzureHttpFunction {
  @js.native
  @JSImport("@azure/functions", "app")
  object App extends js.Object {
    def http(
        name: String,
        appConfig: js.Object
    ): Unit = js.native
  }
}
