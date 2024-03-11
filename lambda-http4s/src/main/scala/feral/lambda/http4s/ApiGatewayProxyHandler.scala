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

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import feral.lambda.ApiGatewayProxyInvocation
import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.ApiGatewayProxyResult
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import fs2.Stream
import org.http4s.Charset
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri

object ApiGatewayProxyHandler {

  def apply[F[_]: Concurrent: ApiGatewayProxyInvocation](
      app: HttpApp[F]): F[Option[ApiGatewayProxyResult]] =
    for {
      event <- Invocation.event
      request <- decodeEvent(event)
      response <- app(request)
      isBase64Encoded = !response.charset.contains(Charset.`UTF-8`)
      responseBody <- response
        .body
        .through(
          if (isBase64Encoded) fs2.text.base64.encode else fs2.text.utf8.decode
        )
        .compile
        .string
    } yield {
      Some(
        ApiGatewayProxyResult(
          response.status.code,
          responseBody,
          isBase64Encoded
        )
      )
    }

  private[http4s] def decodeEvent[F[_]: Concurrent](
      event: ApiGatewayProxyEvent): F[Request[F]] = {
    val queryString: String = getMultiValueQueryStringParameters(
      event.multiValueQueryStringParameters)
    val uriString: String = event.path + (if (queryString.nonEmpty) s"?$queryString" else "")

    for {
      method <- Method.fromString(event.httpMethod).liftTo[F]
      uri <- Uri.fromString(uriString).liftTo[F]
      headers = {
        val builder = List.newBuilder[Header.Raw]
        event.headers.foreach { h => h.foreachEntry(builder += Header.Raw(_, _)) }
        event.multiValueHeaders.foreach { hMap =>
          hMap.foreach {
            case (key, values) =>
              if (!event.headers.exists(_.contains(key))) {
                values.foreach(value => builder += Header.Raw(key, value))
              }
          }
        }
        Headers(builder.result())
      }
      readBody =
        if (event.isBase64Encoded)
          fs2.text.base64.decode[F]
        else
          fs2.text.utf8.encode[F]
    } yield Request(
      method,
      uri,
      headers = headers,
      body = Stream.fromOption[F](event.body).through(readBody)
    )
  }

  private def getMultiValueQueryStringParameters(
      multiValueQueryStringParameters: Option[Map[String, List[String]]]): String =
    multiValueQueryStringParameters.fold("") { params =>
      params
        .flatMap {
          case (key, values) =>
            values.map(value => s"$key=$value")
        }
        .mkString("&")
    }

  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def apply[F[_]: ApiGatewayProxyInvocationV2: Concurrent](
      routes: HttpRoutes[F]): F[Option[ApiGatewayProxyStructuredResultV2]] = httpRoutes(routes)

  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def httpRoutes[F[_]: Concurrent: ApiGatewayProxyInvocationV2](
      routes: HttpRoutes[F]): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandlerV2.httpRoutes(routes)

  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def httpApp[F[_]: Concurrent: ApiGatewayProxyInvocationV2](
      app: HttpApp[F]): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandlerV2(app)
}
