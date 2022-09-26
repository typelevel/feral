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

import cats.syntax.all._
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe.{jsonEncoderWithPrinter, jsonOf}

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import org.http4s.Uri
import org.http4s.client.dsl.Http4sClientDsl
import io.circe._

object FeralLambdaRuntime {

  def apply[F[_]](client: Client[F])(handler: (Json, Context[F]) => F[Json])(implicit F: Concurrent[F]): Resource[F, Unit] = {
    F.background {
      val runtimeUrl = getRuntimeUrl(LambdaEnvironmentVariables.AWS_LAMBDA_RUNTIME_API)
      implicit val lambdaRequestDecoder = jsonOf[F, LambdaRequest]
      implicit val jsonEncoder = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true)) // F is sus, maybe doesnt have conc?
      val http4sClientDsl = new Http4sClientDsl[F] {}
      import http4sClientDsl._
      (for {
        request <- client.expect[LambdaRequest](runtimeUrl)(lambdaRequestDecoder)
        context = createContext(request)
        result <- handler(request.body, context)
        invocationUrl = getInvocationUrl(LambdaEnvironmentVariables.AWS_LAMBDA_RUNTIME_API, request.id)
        _ <- client.successful(POST(result, invocationUrl)(jsonEncoder))
      } yield ()).foreverM
    }
  }.as(()) //needs error handling 
  
  def createContext[F[_]](request: LambdaRequest)(implicit F: Concurrent[F]): Context[F] =
    new Context[F](
      LambdaEnvironmentVariables.AWS_LAMBDA_FUNCTION_NAME,
      LambdaEnvironmentVariables.AWS_LAMBDA_FUNCTION_VERSION,
      request.invokedFunctionArn,
      LambdaEnvironmentVariables.AWS_LAMBDA_FUNCTION_MEMORY_SIZE,
      request.id,
      LambdaEnvironmentVariables.AWS_LAMBDA_LOG_GROUP_NAME,
      LambdaEnvironmentVariables.AWS_LAMBDA_LOG_STREAM_NAME,
      None, //need
      None, //need
      F.pure(FiniteDuration(request.deadlineTimeInMs.toEpochMilli, TimeUnit.MILLISECONDS)) // most likely wrong
    )

  val LAMBDA_VERSION_DATE = "2018-06-01" //is this date correct?
  def getRuntimeUrl(api: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/invocation/next") //should be uri
  def getInvocationUrl(api: String, id: String) = Uri.unsafeFromString(s"http://$api/$LAMBDA_VERSION_DATE/runtime/invocation/$id/response")
  def LAMBDA_INIT_ERROR_URL_TEMPLATE = Uri.unsafeFromString(s"http://{0}/{1}/runtime/init/error")
  def LAMBDA_ERROR_URL_TEMPLATE = Uri.unsafeFromString(s"http://{0}/{1}/runtime/invocation/{2}/error")
}
