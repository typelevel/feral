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

import com.comcast.ip4s.Hostname
import io.circe.Decoder

import java.time.Instant

import codecs.decodeInstant
import codecs.decodeHostname

sealed abstract class ApiGatewayV2WebSocketEvent {
  def stageVariables: Option[Map[String, String]]
  def requestContext: WebSocketRequestContext
  def body: Option[String]
  def isBase64Encoded: Boolean
}

object ApiGatewayV2WebSocketEvent {
  def apply(
      stageVariables: Option[Map[String, String]],
      requestContext: WebSocketRequestContext,
      body: Option[String],
      isBase64Encoded: Boolean
  ): ApiGatewayV2WebSocketEvent =
    new Impl(
      stageVariables,
      requestContext,
      body,
      isBase64Encoded
    )

  implicit val decoder: Decoder[ApiGatewayV2WebSocketEvent] = Decoder.forProduct4(
    "stageVariables",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(ApiGatewayV2WebSocketEvent.apply)

  private final case class Impl(
      stageVariables: Option[Map[String, String]],
      requestContext: WebSocketRequestContext,
      body: Option[String],
      isBase64Encoded: Boolean
  ) extends ApiGatewayV2WebSocketEvent {
    override def productPrefix = "ApiGatewayV2WebSocketEvent"
  }
}

sealed abstract class WebSocketEventType

object WebSocketEventType {
  case object Connect extends WebSocketEventType
  case object Message extends WebSocketEventType
  case object Disconnect extends WebSocketEventType

  private[events] implicit val decoder: Decoder[WebSocketEventType] =
    Decoder.decodeString.map {
      case "CONNECT" => Connect
      case "MESSAGE" => Message
      case "DISCONNECT" => Disconnect
    }
}

sealed abstract class WebSocketRequestContext {
  def stage: String
  def requestId: String
  def apiId: String
  def connectedAt: Instant
  def connectionId: String
  def domainName: Hostname
  def eventType: WebSocketEventType
  def extendedRequestId: String
  def messageId: Option[String]
  def requestTime: Instant
  def routeKey: String
}

object WebSocketRequestContext {
  def apply(
      stage: String,
      requestId: String,
      apiId: String,
      connectedAt: Instant,
      connectionId: String,
      domainName: Hostname,
      eventType: WebSocketEventType,
      extendedRequestId: String,
      messageId: Option[String],
      requestTime: Instant,
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
      messageId,
      requestTime,
      routeKey
    )

  private[events] implicit val decoder: Decoder[WebSocketRequestContext] = Decoder.forProduct11(
    "stage",
    "requestId",
    "apiId",
    "connectedAt",
    "connectionId",
    "domainName",
    "eventType",
    "extendedRequestId",
    "messageId",
    "requestTimeEpoch",
    "routeKey"
  )(WebSocketRequestContext.apply)

  private final case class Impl(
      stage: String,
      requestId: String,
      apiId: String,
      connectedAt: Instant,
      connectionId: String,
      domainName: Hostname,
      eventType: WebSocketEventType,
      extendedRequestId: String,
      messageId: Option[String],
      requestTime: Instant,
      routeKey: String
  ) extends WebSocketRequestContext {
    override def productPrefix = "WebSocketRequestContext"
  }
}
