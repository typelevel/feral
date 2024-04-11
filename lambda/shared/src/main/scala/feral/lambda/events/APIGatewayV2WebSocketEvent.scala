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

import io.circe.Decoder


sealed abstract class APIGatewayV2WebSocketEvent:
  def resource: String
  def path: String
  def httpMethod: String
  def headers: Map[String, String]
  def multiValueHeaders: Map[String, List[String]]
  def queryStringParameters: Map[String, String]
  def multiValueQueryStringParameters: Map[String, List[String]]
  def pathParameters: Map[String, String]
  def stageVariables: Map[String, String]
  def requestContext: RequestContext
  def body: String
  def isBase64Encoded: Boolean 

object APIGatewayV2WebSocketEvent:
  def apply(
    resource: String,
    path: String,
    httpMethod: String,
    headers: Map[String, String],
    multiValueHeaders: Map[String, List[String]],
    queryStringParameters: Map[String, String],
    multiValueQueryStringParameters: Map[String, List[String]],
    pathParameters: Map[String, String],
    stageVariables: Map[String, String],
    requestContext: RequestContext,
    body: String,
    isBase64Encoded: Boolean
  ): APIGatewayV2WebSocketEvent =
    new Impl(
      resource,
      path,
      httpMethod,
      headers,
      multiValueHeaders,
      queryStringParameters,
      multiValueQueryStringParameters,
      pathParameters,
      stageVariables,
      requestContext,
      body,
      isBase64Encoded
    )
  
  implicit val decoder: Decoder[APIGatewayV2WebSocketEvent] = Decoder.forProduct12(
    "resource",
    "path",
    "httpMethod",
    "headers",
    "multiValueHeaders",
    "queryStringParameters",
    "multiValueQueryStringParameters",
    "pathParameters",
    "stageVariables",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(APIGatewayV2WebSocketEvent.apply)

  private final case class Impl(
    resource: String,
    path: String,
    httpMethod: String,
    headers: Map[String, String],
    multiValueHeaders: Map[String, List[String]],
    queryStringParameters: Map[String, String],
    multiValueQueryStringParameters: Map[String, List[String]],
    pathParameters: Map[String, String],
    stageVariables: Map[String, String],
    requestContext: RequestContext,
    body: String,
    isBase64Encoded: Boolean
  ) extends APIGatewayV2WebSocketEvent:
    override def productPrefix = "APIGatewayV2WebSocketEvent"

sealed abstract class RequestIdentity:
  def cognitoIdentityPoolId: String
  def accountId: String
  def cognitoIdentityId: String
  def caller: String
  def apiKey: String
  def sourceIp: String
  def cognitoAuthenticationType: String
  def cognitoAuthenticationProvider: String
  def userArn: String
  def userAgent: String
  def user: String
  def accessKey: String

object RequestIdentity:
  def apply(
    cognitoIdentityPoolId: String,
    accountId: String,
    cognitoIdentityId: String,
    caller: String,
    apiKey: String,
    sourceIp: String,
    cognitoAuthenticationType: String,
    cognitoAuthenticationProvider: String,
    userArn: String,
    userAgent: String,
    user: String,
    accessKey: String
  ): RequestIdentity =
    new Impl(
      cognitoIdentityPoolId,
      accountId,
      cognitoIdentityId,
      caller,
      apiKey,
      sourceIp,
      cognitoAuthenticationType,
      cognitoAuthenticationProvider,
      userArn,
      userAgent,
      user,
      accessKey
    )
  
  private[events] implicit val decoder: Decoder[RequestIdentity] = Decoder.forProduct12(
    "cognitoIdentityPoolId",
    "accountId",
    "cognitoIdentityId",
    "caller",
    "apiKey",
    "sourceIp",
    "cognitoAuthenticationType",
    "cognitoAuthenticationProvider",
    "userArn",
    "userAgent",
    "user",
    "accessKey"
  )(RequestIdentity.apply)

  private final case class Impl(
    cognitoIdentityPoolId: String,
    accountId: String,
    cognitoIdentityId: String,
    caller: String,
    apiKey: String,
    sourceIp: String,
    cognitoAuthenticationType: String,
    cognitoAuthenticationProvider: String,
    userArn: String,
    userAgent: String,
    user: String,
    accessKey: String
  ) extends RequestIdentity:
    override def productPrefix = "RequestIdentity"

sealed abstract class RequestContext:
  def accountId: String
  def resourceId: String
  def stage: String
  def requestId: String
  def identity: RequestIdentity
  def ResourcePath: String
  def authorizer: Map[String, Object]
  def httpMethod: String
  def apiId: String
  def connectedAt: Long
  def connectionId: String
  def domainName: String
  def error: String
  def eventType: String
  def extendedRequestId: String
  def integrationLatency: String
  def messageDirection: String
  def messageId: String
  def requestTime: String
  def requestTimeEpoch: Long
  def routeKey: String
  def status: String

object RequestContext:
  def apply(
    accountId: String,
    resourceId: String,
    stage: String,
    requestId: String,
    identity: RequestIdentity,
    ResourcePath: String,
    authorizer: Map[String, Object],
    httpMethod: String,
    apiId: String,
    connectedAt: Long,
    connectionId: String,
    domainName: String,
    error: String,
    eventType: String,
    extendedRequestId: String,
    integrationLatency: String,
    messageDirection: String,
    messageId: String,
    requestTime: String,
    requestTimeEpoch: Long,
    routeKey: String,
    status: String
  ): RequestContext =
    new Impl(
      accountId,
      resourceId,
      stage,
      requestId,
      identity,
      ResourcePath,
      authorizer,
      httpMethod,
      apiId,
      connectedAt,
      connectionId,
      domainName,
      error,
      eventType,
      extendedRequestId,
      integrationLatency,
      messageDirection,
      messageId,
      requestTime,
      requestTimeEpoch,
      routeKey,
      status
    )
  
  private[events] implicit val decoder: Decoder[RequestContext] = Decoder.forProduct22(
    "accountId",
    "resourceId",
    "stage",
    "requestId",
    "identity",
    "ResourcePath",
    "author",
    "httpMethod",
    "apiId",
    "connectedAt",
    "connectionId",
    "domainName",
    "error",
    "eventType",
    "extendedRequestId",
    "integrationLatency",
    "messageDirection",
    "messageId",
    "requestTime",
    "requestTimeEpoch",
    "routeKey",
    "status"
  )(RequestContext.apply)

  private final case class Impl(
    accountId: String,
    resourceId: String,
    stage: String,
    requestId: String,
    identity: RequestIdentity,
    ResourcePath: String,
    authorizer: Map[String, Object],
    httpMethod: String,
    apiId: String,
    connectedAt: Long,
    connectionId: String,
    domainName: String,
    error: String,
    eventType: String,
    extendedRequestId: String,
    integrationLatency: String,
    messageDirection: String,
    messageId: String,
    requestTime: String,
    requestTimeEpoch: Long,
    routeKey: String,
    status: String
  ) extends RequestContext:
    override def productPrefix = "RequestContext"