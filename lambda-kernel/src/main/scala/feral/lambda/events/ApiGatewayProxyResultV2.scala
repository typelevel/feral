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

package feral.lambda.events

import io.circe.Encoder

final case class ApiGatewayProxyStructuredResultV2(
    statusCode: Int,
    headers: Map[String, String],
    body: String,
    isBase64Encoded: Boolean,
    cookies: List[String]
)

object ApiGatewayProxyStructuredResultV2 {
  implicit def encoder: Encoder[ApiGatewayProxyStructuredResultV2] = Encoder.forProduct5(
    "statusCode",
    "headers",
    "body",
    "isBase64Encoded",
    "cookies"
  )(r => (r.statusCode, r.headers, r.body, r.isBase64Encoded, r.cookies))
}
