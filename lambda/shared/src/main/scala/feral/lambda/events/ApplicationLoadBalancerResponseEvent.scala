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
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import org.typelevel.ci.CIString

sealed abstract class ApplicationLoadBalancerResponseEvent {
  def statusCode: Int
  def statusDescription: Option[String]
  def headers: Option[Map[CIString, String]]
  def multiValueHeaders: Option[Map[CIString, List[String]]]
  def body: Option[String]
  def isBase64Encoded: Boolean
}

object ApplicationLoadBalancerResponseEvent {
  implicit val ciStringKeyEncoder: KeyEncoder[CIString] =
    KeyEncoder.encodeKeyString.contramap(_.toString)
  implicit val ciStringKeyDecoder: KeyDecoder[CIString] =
    KeyDecoder.decodeKeyString.map(CIString(_))

  private implicit val mapStringEncoder: Encoder[Map[CIString, String]] =
    Encoder.encodeMap[CIString, String]
  private implicit val mapListEncoder: Encoder[Map[CIString, List[String]]] =
    Encoder.encodeMap[CIString, List[String]]

  def apply(
      statusCode: Int,
      statusDescription: Option[String],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Boolean
  ): ApplicationLoadBalancerResponseEvent =
    Impl(statusCode, statusDescription, headers, multiValueHeaders, body, isBase64Encoded)

  implicit val encoder: Encoder[ApplicationLoadBalancerResponseEvent] =
    Encoder.forProduct6(
      "statusCode",
      "statusDescription",
      "headers",
      "multiValueHeaders",
      "body",
      "isBase64Encoded"
    )(x =>
      (
        x.statusCode,
        x.statusDescription,
        x.headers,
        x.multiValueHeaders,
        x.body,
        x.isBase64Encoded
      ))

  implicit val decoder: Decoder[ApplicationLoadBalancerResponseEvent] =
    Decoder.forProduct6(
      "statusCode",
      "statusDescription",
      "headers",
      "multiValueHeaders",
      "body",
      "isBase64Encoded"
    )(ApplicationLoadBalancerResponseEvent.apply)

  private final case class Impl(
      statusCode: Int,
      statusDescription: Option[String],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Boolean
  ) extends ApplicationLoadBalancerResponseEvent
}
