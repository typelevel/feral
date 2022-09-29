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
import cats.effect.kernel.Sync
import io.circe.Json
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe.jsonEncoderWithPrinter
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import org.http4s.{EntityEncoder, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import io.circe._
import cats.effect.kernel.Async
import cats.effect.std.Env
import org.http4s.implicits.http4sLiteralsSyntax

// TODO apply function error handling
  // How to run global or static code from handler?
// TODO Custom AWS header models
// TODO CognitoIdentity/ClientContext JSON encoding

object FeralLambdaRuntime {

  final val ApiVersion = "2018-06-01"

  def apply[F[_]](client: Client[F])(handler: (Json, Context[F]) => F[Json])(implicit F: Async[F]): F[Unit] = {
    implicit val lambdaEnv: LambdaRuntimeEnv[F] = LambdaRuntimeEnv(Env.make) // maybe better way
    implicit val jsonEncoder: EntityEncoder[F, Json] = jsonEncoderWithPrinter[F](Printer.noSpaces.copy(dropNullValues = true))
    val http4sClientDsl = new Http4sClientDsl[F] {}
    import http4sClientDsl._
    (for {
      runtimeApi <- lambdaEnv.lambdaRuntimeApi
      request <- client.get(getRuntimeUrl(runtimeApi))(LambdaRequest.fromResponse)
      context <- createContext(request)
      result <- handler(request.body, context)
      invocationUrl = getInvocationUrl(LambdaRuntimeEnv.AWS_LAMBDA_RUNTIME_API, request.id)
      _ <- client.successful(POST(result, invocationUrl))
    } yield ()).foreverM
  }.void
  
  private def createContext[F[_]](request: LambdaRequest)(implicit F: Sync[F], lambdaEnv: LambdaRuntimeEnv[F]): F[Context[F]] = for {
    functionName <- lambdaEnv.lambdaFunctionName
    functionVersion <- lambdaEnv.lambdaFunctionVersion
    functionMemorySize <- lambdaEnv.lambdaFunctionMemorySize
    logGroupName <- lambdaEnv.lambdaLogGroupName
    logStreamName <- lambdaEnv.lambdaLogStreamName
  } yield {
    new Context[F](
      functionName,
      functionVersion,
      request.invokedFunctionArn,
      functionMemorySize,
      request.id,
      logGroupName,
      logStreamName,
      None,
      None,
      F.realTimeInstant.map(curTime => FiniteDuration(request.deadlineTimeInMs.toEpochMilli - curTime.toEpochMilli, TimeUnit.MILLISECONDS))/// how to provide test version?, maybe separate Clock parameter?
    )
  }

  private def getRuntimeUrl(api: String) = uri"http://$api/$ApiVersion/runtime/invocation/next"

  private def getInvocationUrl(api: String, id: String) = uri"http://$api/$ApiVersion/runtime/invocation/$id/response"

  private def getInitErrorUrl(api: String) = uri"http://$api/$ApiVersion/runtime/init/error"

  private def getInvocationErrorUrl(api: String, errorType: String) = uri"http://$api/$ApiVersion/runtime/invocation/$errorType/error"

}
