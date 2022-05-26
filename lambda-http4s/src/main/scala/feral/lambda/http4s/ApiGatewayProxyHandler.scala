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
package http4s

import cats.data.OptionT
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import fs2.Chunk
import org.http4s.Charset
import org.http4s.Entity
import org.http4s.Headers
import org.http4s.HttpRoutes
import org.http4s.MalformedMessageBodyFailure
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.headers.Cookie
import org.http4s.headers.`Set-Cookie`
import scodec.bits.ByteVector

object ApiGatewayProxyHandler {

  def apply[F[_]: Concurrent: ApiGatewayProxyLambdaEnv](
      routes: HttpRoutes[F]): F[Option[ApiGatewayProxyStructuredResultV2]] =
    for {
      event <- LambdaEnv.event
      request <- decodeEvent(event)
      response <- routes(request).getOrElse(Response.notFound)
      isBase64Encoded = !response.charset.contains(Charset.`UTF-8`)
      responseBody <- response.entity match {
        case Entity.Empty => "".pure
        case Entity.Strict(chunk) => // TODO
          if (isBase64Encoded) chunk.toByteVector.toBase64.pure
          else chunk.toByteVector.decodeUtf8.liftTo
        case Entity.Default(body, _) =>
          body
            .through(
              if (isBase64Encoded) fs2.text.base64.encode else fs2.text.utf8.decode
            )
            .compile
            .string
      }
    } yield {
      val headers = response.headers.headers.groupMap(_.name)(_.value)
      Some(
        ApiGatewayProxyStructuredResultV2(
          response.status.code,
          (headers - `Set-Cookie`.name).map {
            case (name, values) =>
              name.toString -> values.mkString(",")
          },
          responseBody,
          isBase64Encoded,
          headers.getOrElse(`Set-Cookie`.name, Nil)
        )
      )
    }

  private[http4s] def decodeEvent[F[_]: Concurrent](
      event: ApiGatewayProxyEventV2): F[Request[F]] = for {
    method <- Method.fromString(event.requestContext.http.method).liftTo[F]
    uri <- Uri.fromString(event.rawPath + "?" + event.rawQueryString).liftTo[F]
    cookies = event.cookies.filter(_.nonEmpty).map(Cookie.name.toString -> _.mkString("; "))
    headers = Headers(cookies.toList ::: event.headers.toList)
    body <- OptionT
      .fromOption(event.body)
      .semiflatMap { eventBody =>
        if (event.isBase64Encoded)
          ByteVector
            .fromBase64Descriptive(eventBody)
            .leftMap(MalformedMessageBodyFailure(_))
            .liftTo
        else
          ByteVector.encodeUtf8(eventBody).liftTo
      }
      .value
  } yield Request(
    method,
    uri,
    headers = headers,
    entity = body.foldMap(b => Entity.strict(Chunk.byteVector(b)))
  )
}
