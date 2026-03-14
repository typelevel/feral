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
package events

import com.comcast.ip4s.Hostname
import feral.lambda.KernelSource
import io.circe.Decoder
import natchez.Kernel
import org.typelevel.ci.CIString

import codecs.decodeHostname
import codecs.decodeKeyCIString

sealed abstract class RequestContext {
  def resourceId: String
  def resourcePath: String
  def httpMethod: String
  def extendedRequestId: String
  def requestTime: String
  def path: String
  def accountId: String
  def protocol: String
  def stage: String
  def domainPrefix: String
  def requestTimeEpoch: Long
  def requestId: String
  def identity: Map[String, Option[String]]
  def domainName: Hostname
  def deploymentId: String
  def apiId: String
}

object RequestContext {

  def apply(
      resourceId: String,
      resourcePath: String,
      httpMethod: String,
      extendedRequestId: String,
      requestTime: String,
      path: String,
      accountId: String,
      protocol: String,
      stage: String,
      domainPrefix: String,
      requestTimeEpoch: Long,
      requestId: String,
      identity: Map[String, Option[String]],
      domainName: Hostname,
      deploymentId: String,
      apiId: String
  ): RequestContext =
    new Impl(
      resourceId,
      resourcePath,
      httpMethod,
      extendedRequestId,
      requestTime,
      path,
      accountId,
      protocol,
      stage,
      domainPrefix,
      requestTimeEpoch,
      requestId,
      identity,
      domainName,
      deploymentId,
      apiId
    )

  implicit def decoder: Decoder[RequestContext] = Decoder.forProduct16(
    "resourceId",
    "resourcePath",
    "httpMethod",
    "extendedRequestId",
    "requestTime",
    "path",
    "accountId",
    "protocol",
    "stage",
    "domainPrefix",
    "requestTimeEpoch",
    "requestId",
    "identity",
    "domainName",
    "deploymentId",
    "apiId"
  )(RequestContext.apply)

  private case class Impl(
      resourceId: String,
      resourcePath: String,
      httpMethod: String,
      extendedRequestId: String,
      requestTime: String,
      path: String,
      accountId: String,
      protocol: String,
      stage: String,
      domainPrefix: String,
      requestTimeEpoch: Long,
      requestId: String,
      identity: Map[String, Option[String]],
      domainName: Hostname,
      deploymentId: String,
      apiId: String
  ) extends RequestContext {
    override def productPrefix = "RequestContext"
  }
}

sealed abstract class ApiGatewayCustomAuthorizerEvent {
  def `type`: String
  def methodArn: String
  def resource: String
  def path: String
  def httpMethod: String
  def headers: Option[Map[CIString, String]]
  def multiValueHeaders: Map[CIString, List[String]]
  def queryStringParameters: Map[CIString, Option[String]]
  def multiValueQueryStringParameters: Map[CIString, Option[List[String]]]
  def pathParameters: Map[CIString, String]
  def stageVariables: Map[CIString, String]
  def requestContext: RequestContext
}

object ApiGatewayCustomAuthorizerEvent {

  def apply(
      `type`: String,
      methodArn: String,
      resource: String,
      path: String,
      httpMethod: String,
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Map[CIString, List[String]],
      queryStringParameters: Map[CIString, Option[String]],
      multiValueQueryStringParameters: Map[CIString, Option[List[String]]],
      pathParameters: Map[CIString, String],
      stageVariables: Map[CIString, String],
      requestContext: RequestContext
  ): ApiGatewayCustomAuthorizerEvent =
    new Impl(
      `type`,
      methodArn,
      resource,
      path,
      httpMethod,
      headers,
      multiValueHeaders,
      queryStringParameters,
      multiValueQueryStringParameters,
      pathParameters,
      stageVariables,
      requestContext
    )

  implicit def kernelSource: KernelSource[ApiGatewayCustomAuthorizerEvent] =
    e => Kernel(e.headers.getOrElse(Map.empty))

  implicit def decoder: Decoder[ApiGatewayCustomAuthorizerEvent] = Decoder.forProduct12(
    "type",
    "methodArn",
    "resource",
    "path",
    "httpMethod",
    "headers",
    "multiValueHeaders",
    "queryStringParameters",
    "multiValueQueryStringParameters",
    "pathParameters",
    "stageVariables",
    "requestContext"
  )(ApiGatewayCustomAuthorizerEvent.apply)

  private case class Impl(
      `type`: String,
      methodArn: String,
      resource: String,
      path: String,
      httpMethod: String,
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Map[CIString, List[String]],
      queryStringParameters: Map[CIString, Option[String]],
      multiValueQueryStringParameters: Map[CIString, Option[List[String]]],
      pathParameters: Map[CIString, String],
      stageVariables: Map[CIString, String],
      requestContext: RequestContext
  ) extends ApiGatewayCustomAuthorizerEvent {
    override def productPrefix = "ApiGatewayCustomAuthorizerEvent"
  }
}
