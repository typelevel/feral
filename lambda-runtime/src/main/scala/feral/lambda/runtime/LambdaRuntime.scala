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
import org.http4s.Method.{GET, POST}
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s._
import io.circe._
import cats.effect.kernel.Outcome._
import io.circe.syntax.EncoderOps

import scala.concurrent.CancellationException

object LambdaRuntime {

  final val ApiVersion = "2018-06-01"

  def apply[F[_]](client: Client[F])(handlerResource: Resource[F, (Json, Context[F]) => F[Json]])(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Unit] =
    handlerResource.attempt.use { handlerOrError =>
      for {
        api <- env.lambdaRuntimeApi
        runtimeUri <- Uri.fromString(s"/$api/$ApiVersion/runtime/").liftTo[F]
        _ <- handlerOrError.fold(handleInitError(runtimeUri, client, _), processEvents(runtimeUri, client, _))
      } yield ()
    }

  private[runtime] def processEvents[F[_]](runtimeUri: Uri, client: Client[F], handler: (Json, Context[F]) => F[Json])(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Unit] = {
    implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    val nextInvocationUri = runtimeUri / "invocation" / "next"
    (for {
      request <- client.get(nextInvocationUri)(LambdaRequest.fromResponse[F])
      invocationErrorUri = runtimeUri / "invocation" / request.id / "error"
      _ <- (for {
        context <- createContext(request)
        handlerFiber <- handler(request.body, context).start
        result <- handlerFiber.join.flatMap {
          case Succeeded(result: F[Json]) => result
          case Errored(e: Throwable) => F.raiseError[Json](e)
          case Canceled() => F.raiseError[Json](new CancellationException)
        }
        invocationResponseUri = runtimeUri / "invocation" / request.id / "response"
        _ <- client.expect[Unit](Request(POST, invocationResponseUri).withEntity(result))
      } yield ()).handleErrorWith(e => {
          val error = LambdaErrorBody(e.getMessage, "Exception", List())
          client.expect[Unit](Request(POST, invocationErrorUri).withEntity(error.asJson))
        })
    } yield ()).foreverM
  }

  private[runtime] def handleInitError[F[_]](runtimeUri: Uri, client: Client[F], e: Throwable)(implicit F: Temporal[F]): F[Unit] = {
    implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    val initErrorUri = runtimeUri / "init" / "error"
    val error = LambdaErrorBody(e.getMessage, "Exception", List())
    client.expect[Unit](Request(POST, initErrorUri).withEntity(error.asJson)) >> F.raiseError[Unit](e)
  }

  private[runtime] def createContext[F[_]](request: LambdaRequest)(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Context[F]] = for {
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
