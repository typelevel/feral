/*
 * Copyright 2023 Typelevel
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

package feral.scalafix

import scalafix.v1._

class V0_3_0Rewrites extends SemanticRule("V0_3_0Rewrites") {
  override def fix(implicit doc: SemanticDocument): Patch =
    Patch.replaceSymbols(
      "feral.lambda.LambdaEnv" -> "feral.lambda.Invocation",
      "feral.lambda.ApiGatewayProxyLambdaEnv" -> "feral.lambda.ApiGatewayProxyInvocationV2",
      "feral.lambda.DynamoDbStreamLambdaEnv" -> "feral.lambda.DynamoDbStreamInvocation",
      "feral.lambda.S3BatchLambdaEnv" -> "feral.lambda.S3BatchInvocation",
      "feral.lambda.SnsLambdaEnv" -> "feral.lambda.SnsInvocation",
      "feral.lambda.SqsLambdaEnv" -> "feral.lambda.SqsInvocation",
      "feral.lambda.events.APIGatewayProxyRequestEvent" -> "feral.lambda.events.ApiGatewayProxyEvent",
      "feral.lambda.events.APIGatewayProxyResponseEvent" -> "feral.lambda.events.ApiGatewayProxyResult",
      "feral.lambda.http4s.ApiGatewayProxyHandler.httpApp" -> "feral.lambda.http4s.ApiGatewayProxyHandlerV2.apply",
      "feral.lambda.http4s.ApiGatewayProxyHandler.apply" -> "feral.lambda.http4s.ApiGatewayProxyHandlerV2.httpRoutes",
      "feral.lambda.http4s.ApiGatewayProxyHandler.httpRoutes" -> "feral.lambda.http4s.ApiGatewayProxyHandlerV2.httpRoutes"
    ) + Patch.removeGlobalImport(Symbol("feral/lambda/http4s/ApiGatewayProxyHandler."))
}
