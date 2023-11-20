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

sealed abstract class ApiGatewayProxyEvent {
  def body: Option[String]
  def resource: String
  def path: String
  def httpMethod: String
  def isBase64Encoded: Boolean
  def queryStringParameters: Option[Map[String, String]]
  def multiValueQueryStringParameters: Option[Map[String, List[String]]]
  def pathParameters: Option[Map[String, String]]
  def stageVariables: Option[Map[String, String]]
  def headers: Option[Map[CIString, String]]
  def multiValueHeaders: Option[Map[CIString, List[String]]]
}

object ApiGatewayProxyEvent {

  def apply(
      body: Option[String],
      resource: String,
      path: String,
      httpMethod: String,
      isBase64Encoded: Boolean,
      queryStringParameters: Option[Map[String, String]],
      multiValueQueryStringParameters: Option[Map[String, List[String]]],
      pathParameters: Option[Map[String, String]],
      stageVariables: Option[Map[String, String]],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]]): ApiGatewayProxyEvent =
    new Impl(
      body,
      resource,
      path,
      httpMethod,
      isBase64Encoded,
      queryStringParameters,
      multiValueQueryStringParameters,
      pathParameters,
      stageVariables,
      headers,
      multiValueHeaders
    )

  import codecs.decodeKeyCIString
  implicit def decoder: Decoder[ApiGatewayProxyEvent] = Decoder.forProduct11(
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
  )(ApiGatewayProxyEvent.apply)

  implicit def kernelSource: KernelSource[ApiGatewayProxyEvent] =
    e => Kernel(e.headers.getOrElse(Map.empty))

  private final case class Impl(
      body: Option[String],
      resource: String,
      path: String,
      httpMethod: String,
      isBase64Encoded: Boolean,
      queryStringParameters: Option[Map[String, String]],
      multiValueQueryStringParameters: Option[Map[String, List[String]]],
      pathParameters: Option[Map[String, String]],
      stageVariables: Option[Map[String, String]],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]]
  ) extends ApiGatewayProxyEvent {
    override def productPrefix = "ApiGatewayProxyEvent"
  }
}
