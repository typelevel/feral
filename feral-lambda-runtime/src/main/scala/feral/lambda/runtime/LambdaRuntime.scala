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

import cats.syntax.all._
import cats.effect.syntax.all._
import cats.effect.kernel.{Async, Concurrent, Sync}
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe._

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import org.http4s.{EntityEncoder, Request, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import io.circe._
import cats.effect.kernel.Outcome._
import cats.effect.std.Env
import io.circe.syntax.EncoderOps
import org.http4s.implicits.http4sLiteralsSyntax
import scala.concurrent.CancellationException

object FeralLambdaRuntime {

  final val ApiVersion = "2018-06-01"

  // TODO find out where to use initErrorUrl
  def apply[F[_]](client: Client[F])(handler: (Json, Context[F]) => F[Json])(implicit F: Async[F], env: LambdaRuntimeEnv[F]): F[Unit] = {
    implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    (for {
      runtimeApi <- env.lambdaRuntimeApi
      request <- client.get(getRuntimeUrl(runtimeApi))(LambdaRequest.fromResponse)
      context <- createContext(request)
      handlerFiber <- handler(request.body, context).start
      outcome <- handlerFiber.join
      invocationErrorUrl = getInvocationErrorUrl(runtimeApi, request.id)
      result <- outcome match {
        case Succeeded(result: F[Json]) => result
        case Errored(e: Throwable) =>
          val error = LambdaErrorRequest(e.getMessage, "exception", List())
          client.expect[Unit](Request(POST, invocationErrorUrl).withEntity(error.asJson)) >> F.raiseError(e)
        case Canceled() =>
          val error = LambdaErrorRequest("cancelled", "cancellation", List()) // TODO need to think about better messages
          client.expect[Unit](Request(POST, invocationErrorUrl).withEntity(error.asJson)) >> F.raiseError(new CancellationException) // is this correct behaviour
      }
      invocationUrl = getInvocationUrl(runtimeApi, request.id)
      _ <- client.expect[Unit](Request(POST, invocationUrl).withEntity(result))
    } yield ()).foreverM
  }
  
  private def createContext[F[_]](request: LambdaRequest)(implicit F: Sync[F], env: LambdaRuntimeEnv[F]): F[Context[F]] = for {
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

  private def getRuntimeUrl(api: String) = Uri.unsafeFromString("http://$api/$ApiVersion/runtime/invocation/next")

  private def getInvocationUrl(api: String, id: String) = Uri.unsafeFromString("http://$api/$ApiVersion/runtime/invocation/$id/response")

  private def getInitErrorUrl(api: String) = Uri.unsafeFromString("http://$api/$ApiVersion/runtime/init/error")

  private def getInvocationErrorUrl(api: String, requestId: String) = Uri.unsafeFromString("http://$api/$ApiVersion/runtime/invocation/$requestId/error")

}
