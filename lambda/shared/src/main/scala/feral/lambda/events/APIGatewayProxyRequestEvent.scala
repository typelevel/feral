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
package events

import io.circe.Decoder
import natchez.Kernel
import org.typelevel.ci.CIString

final case class APIGatewayProxyRequestEvent(
    body: Option[String],
    resource: String,
    path: String,
    httpMethod: String,
    isBase64Encoded: Boolean,
    queryStringParameters: Option[Map[String, String]],
    multiValueQueryStringParameters: Option[Map[String, List[String]]],
    pathParameters: Option[Map[String, String]],
    stageVariables: Option[Map[String, String]],
    headers: Option[Map[String, String]],
    multiValueHeaders: Option[Map[String, List[String]]]
)

object APIGatewayProxyRequestEvent {

  implicit def decoder: Decoder[APIGatewayProxyRequestEvent] = Decoder.forProduct11(
    "body",
    "resource",
    "path",
    "httpMethod",
    "isBase64Encoded",
    "queryStringParameters",
    "multiValueQueryStringParameters",
    "pathParameters",
    "stageVariables",
    "headers",
    "multiValueHeaders"
  )(APIGatewayProxyRequestEvent.apply)

  implicit def kernelSource: KernelSource[APIGatewayProxyRequestEvent] =
    e =>
      Kernel(
        e.headers.getOrElse(Map.empty).map { case (name, value) => CIString(name) -> value })

}
