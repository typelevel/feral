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

package feral.netlify
import io.circe.Decoder
import cats.data.NonEmptyList

final case class NetlifyHttpEvent(
    rawUrl: String,
    rawQuery: String,
    path: String,
    httpMethod: String,
    headers: Map[String, String],
    multiValueHeaders: Map[String, NonEmptyList[String]],
    queryStringParameters: Map[String, String],
    multiValueQueryStringParameters: Map[String, NonEmptyList[String]],
    body: Option[String],
    isBase64Encoded: Boolean
)

object NetlifyHttpEvent {
  implicit def decoder: Decoder[NetlifyHttpEvent] = Decoder.forProduct10(
    "rawUrl",
    "rawQuery",
    "path",
    "httpMethod",
    "headers",
    "multiValueHeaders",
    "queryStringParameters",
    "multiValueQueryStringParameters",
    "body",
    "isBase64Encoded"
  )(NetlifyHttpEvent.apply)
}

import io.circe.Encoder

final case class NetlifyHttpResult(
    statusCode: Int,
    headers: Map[String, String],
    body: String,
    isBase64Encoded: Boolean
)

object NetlifyHttpResult {
  implicit def encoder: Encoder[NetlifyHttpResult] = Encoder.forProduct4(
    "statusCode",
    "headers",
    "body",
    "isBase64Encoded"
  )(r => (r.statusCode, r.headers, r.body, r.isBase64Encoded))
}
