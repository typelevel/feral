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
import feral.lambda.events.ApiGatewayProxyEventV2
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
import org.http4s.headers.Cookie
import org.http4s.headers.`Set-Cookie`

object ApiGatewayProxyHandler {
  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def apply[F[_]: Concurrent: ApiGatewayProxyInvocationV2](
      routes: HttpRoutes[F]): F[Option[ApiGatewayProxyStructuredResultV2]] = httpRoutes(routes)

  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def httpRoutes[F[_]: Concurrent: ApiGatewayProxyInvocationV2](
      routes: HttpRoutes[F]): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandlerV2.httpRoutes(app)

  @deprecated("Use ApiGatewayProxyHandlerV2", "0.3.0")
  def httpApp[F[_]: Concurrent: ApiGatewayProxyInvocationV2](
      app: HttpApp[F]): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandlerV2(app)
}
