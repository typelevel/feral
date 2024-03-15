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

sealed abstract class ApiGatewayProxyResult {
  def statusCode: Int
  def headers: Map[CIString, String]
  def body: String
  def isBase64Encoded: Boolean
}

object ApiGatewayProxyResult {

  def apply(
      statusCode: Int,
      headers: Map[CIString, String],
      body: String,
      isBase64Encoded: Boolean): ApiGatewayProxyResult =
    new Impl(statusCode, headers, body, isBase64Encoded)

  import codecs.encodeKeyCIString
  implicit def encoder: Encoder[ApiGatewayProxyResult] = Encoder.forProduct4(
    "statusCode",
    "headers",
    "body",
    "isBase64Encoded"
  )(r => (r.statusCode, r.headers, r.body, r.isBase64Encoded))

  private final case class Impl(
      statusCode: Int,
      headers: Map[CIString, String],
      body: String,
      isBase64Encoded: Boolean
  ) extends ApiGatewayProxyResult {
    override def productPrefix = "ApiGatewayProxyResult"
  }
}
