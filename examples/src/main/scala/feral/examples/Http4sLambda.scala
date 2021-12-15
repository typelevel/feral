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

package feral.examples

import cats.effect._
import feral.lambda._
import feral.lambda.events._
import natchez.Trace
import natchez.http4s.NatchezMiddleware
import natchez.noop.NoopEntrypoint
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.syntax.all._

/**
 * For a gentler introduction, look at the `KinesisLambda` first.
 */
object http4sHandler
    extends IOLambda[ApiGatewayProxyEventV2, ApiGatewayProxyStructuredResultV2] {

  def handler = for {
    entrypoint <- Resource.pure(NoopEntrypoint[IO]()) // TODO replace with X-Ray
    client <- EmberClientBuilder.default[IO].build
  } yield { implicit env =>
    TracedHandler(entrypoint) { implicit trace =>
      val tracedClient = NatchezMiddleware.client(client)
      ApiGatewayProxyHandler(myRoutes[IO](tracedClient))
    }
  }

  def myRoutes[F[_]: Concurrent: Trace](client: Client[F]): HttpRoutes[F] = {
    implicit val dsl = Http4sDsl[F]
    import dsl._

    val routes = HttpRoutes.of[F] {
      case GET -> Root / "foo" => Ok("bar")
      case GET -> Root / "joke" => Ok(client.expect[String](uri"icanhazdadjoke.com"))
    }

    NatchezMiddleware.server(routes)
  }

}
