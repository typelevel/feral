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

package feral.functions.util

import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Response

import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders
import feral.functions.facade.JSReadableStream

import cats.syntax.all._

import cats.effect.kernel.Async
import cats.effect.syntax.all._
import cats.effect.std.Queue
import cats.effect.kernel.Fiber
import cats.effect.std.Dispatcher

import org.typelevel.ci.CIString

import scala.scalajs.js

import fs2.Stream

import StreamUtil._

object Parser {
  private[functions] def decodeRequest[F[_]: Async](request: JSRequest): F[Request[F]] = {
    for {
      method <- Method.fromString(request.method).liftTo[F]
      uri <- Uri.fromString(request.url).liftTo[F]
      body = JSReadableStream.toFs2[F](request.body)
      headers = {
        val builder = List.newBuilder[Header.Raw]
        val keys = JSHeaders.keyList(request.headers)

        keys.foreach(k =>
          builder.addOne(Header.Raw(CIString(k), request.headers.get(k).getOrElse(""))))

        Headers(builder.result())
      }
    } yield Request[F](
      method = method,
      uri = uri,
      headers = headers,
      body = body 
    )
  }

  private[functions] def encodeResponse[F[_]: Async](
      response: Response[F],
      dispatcher: Dispatcher[F],
      qBound: Int): F[js.Any] = {
    for {
      q <- Queue.bounded[F, StreamData](qBound)
      streamFiber <- createStreamFiber[F](response.body, q)
      headers <- createHeaders[F](response.headers)
      body <- createBodyStream[F](q, streamFiber, dispatcher)
    } yield {
      js.Dynamic
        .literal(
          status = response.status.code,
          headers = headers,
          body = body
        )
    }
  }

  private def createStreamFiber[F[_]: Async](
      stream: Stream[F, Byte],
      q: Queue[F, StreamData]): F[Fiber[F, Throwable, Unit]] = {
    stream
      .chunks
      .evalMap(chunk => q.offer(Data(chunk)))
      .compile
      .drain
      .attempt
      .flatMap {
        case Right(_) => q.offer(End)
        case Left(e) => q.offer(Error(e))
      }
      .start
  }

  private def createHeaders[F[_]: Async](responseHeaders: Headers): F[js.Dictionary[String]] = {
    val headersList = responseHeaders.headers.map(h => (h.name.toString, h.value))
    js.Dictionary(headersList: _*).pure[F]
  }

  private def createBodyStream[F[_]: Async](
      q: Queue[F, StreamData],
      streamFiber: Fiber[F, Throwable, Unit],
      dispatcher: Dispatcher[F]): F[js.Any] = {
    val readableStream: js.Any = js
      .Dynamic
      .newInstance(js.Dynamic.global.ReadableStream)(
        js.Dynamic
          .literal(
            start = (_: js.Dynamic) => { /*NoOp*/ },
            pull = (controller: js.Dynamic) => {
              val effect = createPullEffect[F](controller, q)
              dispatcher.unsafeToPromise(effect)
            },
            cancel = (_: js.UndefOr[js.Any]) => {
              dispatcher.unsafeToPromise(streamFiber.cancel)
            }
          )
      )

    readableStream.pure[F]
  }

  private def createPullEffect[F[_]: Async](
      controller: js.Dynamic,
      q: Queue[F, StreamData]): F[Unit] = {
    q.take.flatMap {
      case Error(e) => {
        Async[F].delay(controller.error(e.getMessage)).void
      }
      case End => {
        Async[F].delay(controller.close()).void
      }
      case Data(chunk) => {
        Async[F].delay {
          val array = new js.typedarray.Uint8Array(chunk.size)
          chunk.toArray.zipWithIndex.foreach {
            case (byte, index) => array(index) = (byte & 0xff).toShort
          }
          controller.enqueue(array)
        }.void
      }
    }
  }
}
