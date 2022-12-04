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
import cats.effect.kernel.Outcome._

import scala.concurrent.CancellationException

object LambdaRuntime {

  final val ApiVersion = "2018-06-01"

  def apply[F[_]](client: Client[F])(
      handlerResource: Resource[F, (Json, Context[F]) => F[Json]])(
      implicit F: Temporal[F],
      env: LambdaRuntimeEnv[F]): F[Nothing] =
    handlerResource.attempt.use[Nothing] { handlerOrError =>
      env.lambdaRuntimeApi.flatMap[Nothing] { api =>
        val runtimeUri = api / ApiVersion / "runtime"
        handlerOrError.fold(
          handleInitError(runtimeUri, client, _),
          processEvents(runtimeUri, client, _)
        )
      }
    }

  private[this] def processEvents[F[_]](
      runtimeUri: Uri,
      client: Client[F],
      handler: (Json, Context[F]) => F[Json])(
      implicit F: Temporal[F],
      env: LambdaRuntimeEnv[F]): F[Nothing] = {
    implicit val jsonEncoder: EntityEncoder[F, Json] =
      jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    val nextInvocationUri = runtimeUri / "invocation" / "next"
    (for {
      request <- client.get(nextInvocationUri)(LambdaRequest.fromResponse[F])
      invocationErrorUri = runtimeUri / "invocation" / request.id / "error"
      _ <- (for {
        context <- createContext(request)
        handlerFiber <- handler(request.body, context).start
        result <- handlerFiber.join.flatMap {
          case Succeeded(result) => result
          case Errored(e) => F.raiseError[Json](e)
          case Canceled() => F.raiseError[Json](new CancellationException)
        }
        invocationResponseUri = runtimeUri / "invocation" / request.id / "response"
        _ <- client.expect[Unit](Request[F](POST, invocationResponseUri).withEntity(result))
      } yield ()).handleErrorWith { ex =>
        val error = LambdaErrorBody.fromThrowable(ex)
        client.expect[Unit](Request[F](POST, invocationErrorUri).withEntity(error))
      }
    } yield ()).foreverM
  }

  private[this] def handleInitError[F[_]](runtimeUri: Uri, client: Client[F], ex: Throwable)(
      implicit F: Temporal[F]): F[Nothing] = {
    val initErrorUri = runtimeUri / "init" / "error"
    val error = LambdaErrorBody.fromThrowable(ex)
    client
      .expect[Unit](Request[F](POST, initErrorUri).withEntity(error))
      .productR[Nothing](F.raiseError[Nothing](ex))
  }

  private[this] def createContext[F[_]](request: LambdaRequest)(
      implicit F: Temporal[F],
      env: LambdaRuntimeEnv[F]): F[Context[F]] = for {
    functionName <- env.lambdaFunctionName
    functionVersion <- env.lambdaFunctionVersion
    functionMemorySize <- env.lambdaFunctionMemorySize
    logGroupName <- env.lambdaLogGroupName
    logStreamName <- env.lambdaLogStreamName
  } yield {
    new Context[F](
      functionName,
      functionVersion,
      request.invokedFunctionArn,
      functionMemorySize,
      request.id,
      logGroupName,
      logStreamName,
      request.identity,
      request.clientContext,
      F.realTime.map(curTime => request.deadlineTimeInMs - curTime)
    )
  }
}
