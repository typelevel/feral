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
package runtime

import cats.effect.Resource
import cats.effect.Temporal
import cats.effect.syntax.resource._
import cats.syntax.all._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.client.Client

import scala.util.control.NonFatal

import api._

object LambdaRuntime {

  def apply[F[_]: Temporal: LambdaRuntimeEnv, Event: Decoder, Result: Encoder](
      client: Client[F])(
      handler: Resource[F, Invocation[F, Event] => F[Option[Result]]]): F[Nothing] =
    LambdaRuntimeAPIClient(client).flatMap(client =>
      handler.both(LambdaSettings.fromLambdaEnv.toResource).attempt.use[INothing] {
        case Right((handler, settings)) => runloop(client, settings, handler)
        case Left(ex) => client.reportInitError(ex) *> ex.raiseError
      })

  private def runloop[F[_]: Temporal, Event: Decoder, Result: Encoder](
      client: LambdaRuntimeAPIClient[F],
      settings: LambdaSettings,
      run: Invocation[F, Event] => F[Option[Result]]) = {
    client
      .nextInvocation()
      .flatMap(handleSingleRequest(client, settings, run))
      .handleErrorWith {
        case ex @ ContainerError => ex.raiseError[F, Unit]
        case NonFatal(_) => ().pure
        case ex => ex.raiseError
      }
      .foreverM[INothing]
  }
  private def handleSingleRequest[F[_]: Temporal, Event: Decoder, Result: Encoder](
      client: LambdaRuntimeAPIClient[F],
      settings: LambdaSettings,
      run: Invocation[F, Event] => F[Option[Result]])(request: LambdaRequest): F[Unit] = {
    val program = for {
      event <- request.body.as[Event].liftTo[F]
      maybeResult <- run(Invocation.pure(event, Context.from[F](request, settings)))
      _ <- maybeResult.traverse(client.submit(request.id, _))
    } yield ()
    program.handleErrorWith {
      case ex @ ContainerError => ex.raiseError
      case NonFatal(ex) => client.reportInvocationError(request.id, ex)
      case ex => ex.raiseError
    }
  }
}
