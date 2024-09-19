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
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpApp
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.typelevel.ci.CIString

import scala.util.control.NonFatal

object IOCloudHttpFunction {
  private[googlecloud] def fromHttpRequest(request: HttpRequest): IO[Request[IO]] = for {
    method <- Method.fromString(request.getMethod()).liftTo[IO]
    uri <- Uri.fromString(request.getUri()).liftTo[IO]
    headers <- IO {
      val builder = List.newBuilder[Header.Raw]
      request.getHeaders().forEach {
        case (name, values) =>
          values.forEach { value => builder += Header.Raw(CIString(name), value) }
      }
      Headers(builder.result())
    }
    body = fs2.io.readInputStream(IO(request.getInputStream()), 4096)
  } yield Request(
    method,
    uri,
    headers = headers,
    body = body
  )

  private[googlecloud] def writeResponse(
      http4sResponse: Response[IO],
      googleResponse: HttpResponse): IO[Unit] =
    for {
      _ <- IO {
        googleResponse.setStatusCode(http4sResponse.status.code, http4sResponse.status.reason)
      }

      _ <- IO {
        http4sResponse.headers.foreach { header =>
          {
            googleResponse.appendHeader(header.name.toString, header.value)
          }
        }
      }

      _ <- http4sResponse
        .body
        .through(fs2.io.writeOutputStream(IO(googleResponse.getOutputStream())))
        .compile
        .drain

    } yield ()
}

abstract class IOCloudHttpFunction extends HttpFunction {

  protected def runtime: IORuntime = IORuntime.global

  def handler: Resource[IO, HttpApp[IO]]

  private[this] val (dispatcher, handle) = {
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
      .map(_._1) // drop unused finalizer
      .unsafeRunSync()(runtime)
  }

  final def service(request: HttpRequest, response: HttpResponse): Unit = {

    dispatcher.unsafeRunSync(
      IOCloudHttpFunction.fromHttpRequest(request).flatMap { req =>
        handle.flatMap(_(req)).flatMap { res =>
          IOCloudHttpFunction.writeResponse(res, response)
        }
      }
    )
  }
}
