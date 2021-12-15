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
 * For a gentle introduction, please look at the `KinesisLambda` first which uses
 * `IOLambda.Simple`.
 *
 * The `IOLambda` uses a slightly more complicated encoding by introducing an effect
 * `LambdaEnv[F]` which provides access to the event and context in `F`. This allows you to
 * compose your handler as a stack of "middlewares", making it easy to e.g. add tracing to your
 * lambda.
 */
object http4sHandler
    extends IOLambda[ApiGatewayProxyEventV2, ApiGatewayProxyStructuredResultV2] {

  /**
   * Actually, this is a `Resource` that builds your handler: it is acquired exactly once when
   * your lambda starts and is permanently installed to process all incoming events.
   *
   * The handler itself is a program expressed as `IO[Option[Result]]`, which is run every time
   * that your lambda is triggered. This may seem counter-intuitive at first: where does the
   * event come from? Because accessing the event via `LambdaEnv` is now also an effect in `IO`,
   * it becomes a step in your program.
   */
  def handler = for {
    entrypoint <- Resource.pure(NoopEntrypoint[IO]()) // TODO replace with X-Ray
    client <- EmberClientBuilder.default[IO].build
  } yield { implicit env => // the LambdaEnv provides access to the event and context

    // a middleware to add tracing to any handler
    // it extracts the kernel from the event and adds tags derived from the context
    TracedHandler(entrypoint) { implicit trace =>
      val tracedClient = NatchezMiddleware.client(client)

      // a "middleware" that converts an HttpRoutes into a ApiGatewayProxyHandler
      ApiGatewayProxyHandler(myRoutes[IO](tracedClient))
    }
  }

  /**
   * Nothing special about this method (including its existence), just an example :)
   */
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
