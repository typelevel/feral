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

package feral.lambda.apigatewayproxy

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.semiauto

// TODO Just the bare minimum for proof-of-concept
final case class ApiGatewayProxyEventV2(
    rawPath: String,
    rawQueryString: String,
    headers: Map[String, String],
    requestContext: RequestContext,
    body: Option[String],
    isBase64Encoded: Boolean
)

object ApiGatewayProxyEventV2 {
  implicit def decoder: Decoder[ApiGatewayProxyEventV2] = semiauto.deriveDecoder
}

final case class RequestContext(http: Http)

final case class Http(method: String)
