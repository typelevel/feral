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

import cats.effect.Temporal
import cats.syntax.all._
import cats.effect.syntax.all._
import cats.effect.kernel.Resource
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s._
import io.circe._

object LambdaRuntime {

  final val ApiVersion = "2018-06-01"

  def apply[F[_]](client: Client[F])(
      handlerResource: Resource[F, (Json, Context[F]) => F[Json]])(
      implicit F: Temporal[F],
      env: LambdaRuntimeEnv[F]): F[Unit] =
    handlerResource.attempt.use[Unit] { handlerOrError =>
      env.lambdaRuntimeApi.flatMap[Unit] { api =>
        val runtimeUri = api / ApiVersion / "runtime"
        LambdaSettings.fromLambdaEnvironment.attempt.flatMap {
          (handlerOrError, _)
            .tupled
            .fold(
              handleInitError(runtimeUri, client, _),
              {
                case (handler, settings) =>
                  processEvents(runtimeUri, client, handler, settings)
              }
            )
        }
      }
    }
  private[this] def processEvents[F[_]](
      runtimeUri: Uri,
      client: Client[F],
      handler: (Json, Context[F]) => F[Json],
      settings: LambdaSettings)(implicit F: Temporal[F]): F[Unit] = {

    implicit val jsonEncoder: EntityEncoder[F, Json] =
      jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))

    val nextInvocationUri = runtimeUri / "invocation" / "next"

    client
      .get(nextInvocationUri)(LambdaRequest.fromResponse[F])
      .flatMap { request =>
        val context = createContext(request, settings)
        val respond = for {
          handlerFiber <- handler(request.body, context).start
          result <- handlerFiber.join.flatMap(_.embedError)
          invocationResponseUri = runtimeUri / "invocation" / request.id / "response"
          _ <- client.expect[Unit](Request[F](POST, invocationResponseUri).withEntity(result))
        } yield ()

        respond.handleErrorWith { ex =>
          val error = LambdaErrorBody.fromThrowable(ex)
          val invocationErrorUri = runtimeUri / "invocation" / request.id / "error"
          client.expect[Unit](Request[F](POST, invocationErrorUri).withEntity(error))
        }
      }
      .foreverM
  }

  private[this] def handleInitError[F[_]](runtimeUri: Uri, client: Client[F], ex: Throwable)(
      implicit F: Temporal[F]): F[Unit] = {
    val initErrorUri = runtimeUri / "init" / "error"
    val error = LambdaErrorBody.fromThrowable(ex)
    client
      .expect[Unit](Request[F](POST, initErrorUri).withEntity(error))
      .productR[Unit](F.raiseError[Unit](ex))
  }

  private[this] def createContext[F[_]](request: LambdaRequest, settings: LambdaSettings)(
      implicit F: Temporal[F]): Context[F] =
    new Context[F](
      settings.functionName,
      settings.functionVersion,
      request.invokedFunctionArn,
      settings.functionMemorySize,
      request.id,
      settings.logGroupName,
      settings.logStreamName,
      request.identity,
      request.clientContext,
      F.realTime.map(curTime => request.deadlineTime - curTime)
    )

}
