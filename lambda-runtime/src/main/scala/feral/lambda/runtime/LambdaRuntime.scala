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

import cats.{ApplicativeError, MonadThrow}
import cats.effect.{IO, Temporal}
import cats.syntax.all._
import cats.effect.syntax.all._
import cats.effect.kernel.{Async, Concurrent, Resource, Sync}
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.{EntityEncoder, Request, Uri}
import io.circe._
import cats.effect.kernel.Outcome._
import io.circe.syntax.EncoderOps

object FeralLambdaRuntime {

  final val ApiVersion = "2018-06-01"

  def apply[F[_]](client: Client[F])(handler: Resource[F, (Json, Context[F]) => F[Json]])(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Unit] =
    handler.use(handler => env.lambdaRuntimeApi.flatMap(api => processEvents(api, client, handler)))

  private def processEvents[F[_]](runtimeApi: String, client: Client[F], handlerFun: (Json, Context[F]) => F[Json])(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Unit] = {
    implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    (for {
      nextInvocationUrl <- getNextInvocationUrl(runtimeApi)
      request <- client.get(nextInvocationUrl)(LambdaRequest.fromResponse[F])
      context <- createContext(request)
      invocationErrorUrl <- getInvocationErrorUrl(runtimeApi, request.id)
      handlerFiber <- handlerFun(request.body, context).start
      result <- handlerFiber.join.flatMap {
        case Succeeded(result: F[Json]) => Option(result).sequence
        case Errored(e: Throwable) =>
          val error = LambdaErrorRequest(e.getMessage, "exception", List())
          client.expect[Unit](Request(POST, invocationErrorUrl).withEntity(error.asJson)) >> Option.empty[Json].pure[F]
        case Canceled() =>
          val error = LambdaErrorRequest("cancelled", "cancellation", List()) // TODO need to think about better messages
          client.expect[Unit](Request(POST, invocationErrorUrl).withEntity(error.asJson)) >> Option.empty[Json].pure[F]
      }
      invocationResponseUrl <- getInvocationResponseUrl(runtimeApi, request.id)
      _ <- result.map(body => client.expect[Unit](Request(POST, invocationResponseUrl).withEntity(body))).sequence
    } yield ()).foreverM
  }
  
  private def createContext[F[_]](request: LambdaRequest)(implicit F: Temporal[F], env: LambdaRuntimeEnv[F]): F[Context[F]] = for {
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

  private def getNextInvocationUrl[F[_]: MonadThrow](api: String): F[Uri] = Uri.fromString(s"/$api/$ApiVersion/runtime/invocation/next").liftTo[F]

  private def getInvocationResponseUrl[F[_]: MonadThrow](api: String, id: String): F[Uri] = Uri.fromString(s"/$api/$ApiVersion/runtime/invocation/$id/response").liftTo[F]

  private def getInitErrorUrl[F[_]: MonadThrow](api: String): F[Uri] = Uri.fromString(s"/$api/$ApiVersion/runtime/init/error").liftTo[F]

  private def getInvocationErrorUrl[F[_]: MonadThrow](api: String, requestId: String): F[Uri] = Uri.fromString(s"/$api/$ApiVersion/runtime/invocation/$requestId/error").liftTo[F]

}
