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
package apigatewayproxy

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import fs2.Stream
import io.circe.generic.auto._
import org.http4s.Charset
import org.http4s.ContextRequest
import org.http4s.ContextRoutes
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri

abstract class ApiGatewayProxyLambda(implicit runtime: IORuntime)
    extends IOLambda[
      ContextRoutes[Context, IO],
      ApiGatewayProxyEventV2,
      ApiGatewayProxyResultV2] {

  def routes: Resource[IO, ContextRoutes[Context, IO]]

  override final def setup: Resource[IO, ContextRoutes[Context, IO]] = routes

  override final def apply(
      event: ApiGatewayProxyEventV2,
      context: Context,
      routes: ContextRoutes[Context, IO]): IO[Some[ApiGatewayProxyResultV2]] =
    for {
      method <- IO.fromEither(Method.fromString(event.requestContext.http.method))
      uri <- IO.fromEither(Uri.fromString(event.rawPath))
      headers = Headers(event.headers.toList)
      requestBody =
        if (event.isBase64Encoded)
          Stream.fromOption[IO](event.body).through(fs2.text.base64.decode)
        else
          Stream.fromOption[IO](event.body).through(fs2.text.utf8.encode)
      request = Request(method, uri, headers = headers, body = requestBody)
      response <- routes(ContextRequest(context, request)).getOrElse(Response.notFound[IO])
      isBase64Encoded = !response.charset.contains(Charset.`UTF-8`)
      responseBody <- (if (isBase64Encoded)
                         response.body.through(fs2.text.base64.encode)
                       else
                         response.body.through(fs2.text.utf8.decode)).compile.foldMonoid
    } yield Some(
      ApiGatewayProxyStructuredResultV2(
        response.status.code,
        response
          .headers
          .headers
          .map {
            case Header.Raw(name, value) =>
              name.toString -> value
          }
          .toMap,
        responseBody,
        isBase64Encoded
      )
    )

}
