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
import io.circe.Encoder
import org.typelevel.ci.CIString

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/alb.d.ts (ALBResult)
// https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html

sealed abstract class ApplicationLoadBalancerResponseEvent {
  def statusCode: Int
  def statusDescription: Option[String]
  def headers: Option[Map[CIString, String]]
  def multiValueHeaders: Option[Map[CIString, List[String]]]
  def body: Option[String]
  def isBase64Encoded: Option[Boolean]
}

object ApplicationLoadBalancerResponseEvent {

  def apply(
      statusCode: Int,
      statusDescription: Option[String],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Option[Boolean]
  ): ApplicationLoadBalancerResponseEvent =
    new Impl(
      statusCode,
      statusDescription,
      headers,
      multiValueHeaders,
      body,
      isBase64Encoded
    )

  import codecs.decodeKeyCIString
  import codecs.encodeKeyCIString
  implicit val decoder: Decoder[ApplicationLoadBalancerResponseEvent] =
    Decoder.forProduct6(
      "statusCode",
      "statusDescription",
      "headers",
      "multiValueHeaders",
      "body",
      "isBase64Encoded"
    )(ApplicationLoadBalancerResponseEvent.apply)

  implicit val encoder: Encoder[ApplicationLoadBalancerResponseEvent] =
    Encoder.forProduct6(
      "statusCode",
      "statusDescription",
      "headers",
      "multiValueHeaders",
      "body",
      "isBase64Encoded"
    )(r =>
      (
        r.statusCode,
        r.statusDescription,
        r.headers,
        r.multiValueHeaders,
        r.body,
        r.isBase64Encoded
      ))

  private final case class Impl(
      statusCode: Int,
      statusDescription: Option[String],
      headers: Option[Map[CIString, String]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Option[Boolean]
  ) extends ApplicationLoadBalancerResponseEvent {
    override def productPrefix = "ApplicationLoadBalancerResponseEvent"
  }
}
