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
import org.typelevel.ci.CIString

sealed abstract class ApiGatewayProxyStructuredResultV2 {
  def statusCode: Int
  def headers: Map[CIString, String]
  def body: String
  def isBase64Encoded: Boolean
  def cookies: List[String]
}

object ApiGatewayProxyStructuredResultV2 {
  def apply(
      statusCode: Int,
      headers: Map[CIString, String],
      body: String,
      isBase64Encoded: Boolean,
      cookies: List[String]
  ): ApiGatewayProxyStructuredResultV2 =
    new Impl(statusCode, headers, body, isBase64Encoded, cookies)

  import codecs.encodeKeyCIString
  implicit def encoder: Encoder[ApiGatewayProxyStructuredResultV2] = Encoder.forProduct5(
    "statusCode",
    "headers",
    "body",
    "isBase64Encoded",
    "cookies"
  )(r => (r.statusCode, r.headers, r.body, r.isBase64Encoded, r.cookies))

  private final case class Impl(
      statusCode: Int,
      headers: Map[CIString, String],
      body: String,
      isBase64Encoded: Boolean,
      cookies: List[String]
  ) extends ApiGatewayProxyStructuredResultV2 {
    override def productPrefix = "ApiGatewayProxyStructuredResultV2"
  }
}
