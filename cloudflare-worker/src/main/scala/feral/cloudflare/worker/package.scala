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

package feral.cloudflare

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import fs2.Stream
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Request
import org.http4s.Response

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.http4s.Method
import org.http4s.Uri

package object worker {

  @js.native
  @js.annotation.JSGlobal("addEventListener")
  @nowarn
  private[worker] def addEventListener[E <: facade.Event](
      `type`: String,
      listener: js.Function1[E, Unit]): Unit = js.native

  private[worker] def fromRequest[F[_]](req: facade.Request)(implicit F: Async[F]): F[Request[F]] = for {
    method <- F.fromEither(Method.fromString(req.method))
    uri <- F.fromEither(Uri.fromString(req.url))
  } yield Request(method, uri)

  private[worker] def toRequest[F[_]](req: Request[F]): facade.Request = ???

  private[worker] def fromResponse[F[_]](req: facade.Response): F[Response[F]] = ???

  private[worker] def toResponse[F[_]](req: Response[F]): facade.Response = ???

  private[worker] def toDomHeaders(headers: Headers): facade.Headers =
    new facade.Headers(
      headers
        .headers
        .view
        .map {
          case Header.Raw(name, value) =>
            name.toString -> value
        }
        .toMap
        .toJSDictionary)

  private[worker] def fromDomHeaders(headers: facade.Headers): Headers =
    Headers(
      headers.toIterable.map { header => header(0) -> header(1) }.toList
    )

  private[worker] def fromReadableStream[F[_]](
      rs: facade.ReadableStream[js.typedarray.Uint8Array])(
      implicit F: Async[F]): Stream[F, Byte] =
    Stream.bracket(F.delay(rs.getReader()))(r => F.delay(r.releaseLock())).flatMap { reader =>
      Stream.unfoldChunkEval(reader) { reader =>
        F.fromPromise(F.delay(reader.read())).map { chunk =>
          if (chunk.done)
            None
          else
            Some((fs2.Chunk.uint8Array(chunk.value), reader))
        }
      }
    }

  private[worker] def toReadableStream[F[_]](s: Stream[F, Byte]): facade.ReadableStream[js.typedarray.Uint8Array] = ???

  private[worker] def closeReadableStream[F[_], A](
      rs: facade.ReadableStream[A],
      exitCase: Resource.ExitCase)(implicit F: Async[F]): F[Unit] = exitCase match {
    case Resource.ExitCase.Succeeded =>
      F.fromPromise(F.delay(rs.cancel(js.undefined))).void
    case Resource.ExitCase.Errored(ex) =>
      F.fromPromise(F.delay(rs.cancel(ex.toString().asInstanceOf[js.Any]))).void
    case Resource.ExitCase.Canceled =>
      F.fromPromise(F.delay(rs.cancel(js.undefined))).void
  }

}
