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

sealed abstract class ApiGatewayV2WebSocketEvent {
  def stageVariables: Map[String, String]
  def requestContext: WebSocketRequestContext
  def body: String
  def isBase64Encoded: Boolean
}

object APIGatewayV2WebSocketEvent {
  def apply(
      stageVariables: Map[String, String],
      requestContext: WebSocketRequestContext,
      body: String,
      isBase64Encoded: Boolean
  ): APIGatewayV2WebSocketEvent =
    new Impl(
      stageVariables,
      requestContext,
      body,
      isBase64Encoded
    )

  implicit val decoder: Decoder[APIGatewayV2WebSocketEvent] = Decoder.forProduct4(
    "stageVariables",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(APIGatewayV2WebSocketEvent.apply)

  private final case class Impl(
      stageVariables: Map[String, String],
      requestContext: WebSocketRequestContext,
      body: String,
      isBase64Encoded: Boolean
  ) extends APIGatewayV2WebSocketEvent {
    override def productPrefix = "APIGatewayV2WebSocketEvent"
  }
}

sealed abstract class WebSocketRequestContext {
  def stage: String
  def requestId: String
  def apiId: String
  def connectedAt: Long
  def connectionId: String
  def domainName: String
  def eventType: String
  def extendedRequestId: String
  def messageDirection: String
  def messageId: String
  def requestTime: String
  def requestTimeEpoch: Long
  def routeKey: String
}

object WebSocketRequestContext {
  def apply(
      stage: String,
      requestId: String,
      apiId: String,
      connectedAt: Long,
      connectionId: String,
      domainName: String,
      eventType: String,
      extendedRequestId: String,
      messageDirection: String,
      messageId: String,
      requestTime: String,
      requestTimeEpoch: Long,
      routeKey: String
  ): WebSocketRequestContext =
    new Impl(
      stage,
      requestId,
      apiId,
      connectedAt,
      connectionId,
      domainName,
      eventType,
      extendedRequestId,
      messageDirection,
      messageId,
      requestTime,
      requestTimeEpoch,
      routeKey
    )

  private[events] implicit val decoder: Decoder[WebSocketRequestContext] = Decoder.forProduct13(
    "stage",
    "requestId",
    "apiId",
    "connectedAt",
    "connectionId",
    "domainName",
    "eventType",
    "extendedRequestId",
    "messageDirection",
    "messageId",
    "requestTime",
    "requestTimeEpoch",
    "routeKey"
  )(WebSocketRequestContext.apply)

  private final case class Impl(
      stage: String,
      requestId: String,
      apiId: String,
      connectedAt: Long,
      connectionId: String,
      domainName: String,
      eventType: String,
      extendedRequestId: String,
      messageDirection: String,
      messageId: String,
      requestTime: String,
      requestTimeEpoch: Long,
      routeKey: String
  ) extends WebSocketRequestContext {
    override def productPrefix = "WebSocketRequestContext"
  }
}
