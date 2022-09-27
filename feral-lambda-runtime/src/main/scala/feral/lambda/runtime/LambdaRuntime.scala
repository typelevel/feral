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

// TODO implement a runtime here
// it should retrieve incoming events and handle them with the handler
// it will run on a background fiber, whose lifecycle is controlled by the resource

package feral.lambda
package runtime

import cats.Applicative
import cats.syntax.all._
import cats.effect.kernel.{Concurrent, Resource, Sync}
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe.jsonEncoderWithPrinter

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import org.http4s.{EntityEncoder, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import io.circe._

import java.time.Instant //safe to use in native?
import cats.effect.kernel.Async

object FeralLambdaRuntime {

  val LAMBDA_VERSION_DATE = "2018-06-01"

  def apply[F[_]](client: Client[F])(handler: (Json, Context[F]) => F[Json])(implicit F: Async[F]): Resource[F, Unit] = { //already have handler function???
    F.background {
      val runtimeUrl = getRuntimeUrl(LambdaReservedEnvVars.AWS_LAMBDA_RUNTIME_API)
      implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
      val http4sClientDsl = new Http4sClientDsl[F] {}
      import http4sClientDsl._
      (for {
        request <- client.get(runtimeUrl)(LambdaRequest.fromResponse) // unsure how to deal with bad response
        context <- createContext(request)
        result <- handler(request.body, context)
        invocationUrl = getInvocationUrl(LambdaReservedEnvVars.AWS_LAMBDA_RUNTIME_API, request.id)
        _ <- client.successful(POST(result, invocationUrl))
      } yield ()).foreverM
    }
  }.as(()) // how to handle Outcome error and cancellation?
  
  private def createContext[F[_]](request: LambdaRequest)(implicit F: Sync[F]): F[Context[F]] = for {
    functionName <- envVar(LambdaReservedEnvVars.AWS_LAMBDA_FUNCTION_NAME)
    functionVersion <- envVar(LambdaReservedEnvVars.AWS_LAMBDA_FUNCTION_VERSION)
    functionMemorySize <- envVar(LambdaReservedEnvVars.AWS_LAMBDA_FUNCTION_MEMORY_SIZE).map(_.toInt)
    logGroupName <- envVar(LambdaReservedEnvVars.AWS_LAMBDA_LOG_GROUP_NAME)
    logStreamName <- envVar(LambdaReservedEnvVars.AWS_LAMBDA_LOG_STREAM_NAME)
  } yield {
    new Context[F](
      functionName,
      functionVersion,
      request.invokedFunctionArn,
      functionMemorySize,
      request.id,
      logGroupName,
      logStreamName,
      None, //need
      None, //need
      F.delay(FiniteDuration(request.deadlineTimeInMs.toEpochMilli - Instant.now.toEpochMilli, TimeUnit.MILLISECONDS))
    )
  }

  private def getRuntimeUrl(api: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/invocation/next") //need to be unsafe?

  private def getInvocationUrl(api: String, id: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/invocation/$id/response")

  private def getInitErrorUrl(api: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/init/error")

  private def getInvocationErrorUrl(api: String, errorType: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/invocation/$errorType/error")

  private def envVar[F[_]](envVar: String)(implicit F: Sync[F]): F[String] = {
    F.delay(sys.env(envVar))
  }
}
