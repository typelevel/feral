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

import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import fs2.Stream
import natchez.Trace
import org.http4s.Charset
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.typelevel.vault.Key
import org.typelevel.vault.Vault

abstract class ApiGatewayProxyHttp4sLambda
    extends IOLambda[ApiGatewayProxyEventV2, ApiGatewayProxyStructuredResultV2] {

  val ContextKey = Key.newKey[SyncIO, Context].unsafeRunSync()
  val EventKey = Key.newKey[SyncIO, ApiGatewayProxyEventV2].unsafeRunSync()

  def routes: Resource[IO, HttpRoutes[IO]]

  protected type Setup = HttpRoutes[IO]
  protected override final val setup: Resource[IO, HttpRoutes[IO]] = routes

  override final def apply(
      event: ApiGatewayProxyEventV2,
      context: Context,
      routes: HttpRoutes[IO])(
      implicit T: Trace[IO]): IO[Some[ApiGatewayProxyStructuredResultV2]] =
    for {
      method <- IO.fromEither(Method.fromString(event.requestContext.http.method))
      uri <- IO.fromEither(Uri.fromString(event.rawPath))
      headers = Headers(event.headers.toList)
      requestBody =
        if (event.isBase64Encoded)
          Stream.fromOption[IO](event.body).through(fs2.text.base64.decode)
        else
          Stream.fromOption[IO](event.body).through(fs2.text.utf8.encode)
      request = Request(
        method,
        uri,
        headers = headers,
        body = requestBody,
        attributes = Vault.empty.insert(ContextKey, context).insert(EventKey, event))
      response <- routes(request).getOrElse(Response.notFound[IO])
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
