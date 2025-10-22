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
import org.typelevel.ci.CIString

final case class Elb(targetGroupArn: String)

object Elb {
  implicit val decoder: Decoder[Elb] = Decoder.forProduct1("targetGroupArn")(Elb.apply)
}

sealed abstract class ApplicationLoadBalancerRequestContext {
  def elb: Elb
}

object ApplicationLoadBalancerRequestContext {
  import feral.lambda.events.Elb._ // Ensure Decoder[Elb] is in scope

  def apply(elb: Elb): ApplicationLoadBalancerRequestContext =
    Impl(elb)

  implicit val decoder: Decoder[ApplicationLoadBalancerRequestContext] =
    Decoder.forProduct1("elb")(ApplicationLoadBalancerRequestContext.apply)

  private final case class Impl(elb: Elb) extends ApplicationLoadBalancerRequestContext
}

sealed abstract class ApplicationLoadBalancerRequestEvent {
  def requestContext: ApplicationLoadBalancerRequestContext
  def httpMethod: String
  def path: String
  def queryStringParameters: Option[Map[String, String]]
  def headers: Option[Map[CIString, String]]
  def multiValueQueryStringParameters: Option[Map[String, List[String]]]
  def multiValueHeaders: Option[Map[CIString, List[String]]]
  def body: Option[String]
  def isBase64Encoded: Boolean

  /**
   * If isBase64Encoded is true, decodes the body from base64. Otherwise, returns the UTF-8
   * bytes of the body string.
   */
  def decodedBody: Option[Array[Byte]] =
    body.map { b =>
      if (isBase64Encoded) java.util.Base64.getDecoder.decode(b)
      else b.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    }
}

object ApplicationLoadBalancerRequestEvent {
  private implicit val ciStringKeyDecoder: io.circe.KeyDecoder[CIString] =
    io.circe.KeyDecoder.instance(s => Some(CIString(s)))

  private implicit val mapStringDecoder: Decoder[Map[CIString, String]] =
    Decoder.decodeMap[CIString, String]
  private implicit val mapListDecoder: Decoder[Map[CIString, List[String]]] =
    Decoder.decodeMap[CIString, List[String]]

  def apply(
      requestContext: ApplicationLoadBalancerRequestContext,
      httpMethod: String,
      path: String,
      queryStringParameters: Option[Map[String, String]],
      headers: Option[Map[CIString, String]],
      multiValueQueryStringParameters: Option[Map[String, List[String]]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Boolean
  ): ApplicationLoadBalancerRequestEvent =
    Impl(
      requestContext,
      httpMethod,
      path,
      queryStringParameters,
      headers,
      multiValueQueryStringParameters,
      multiValueHeaders,
      body,
      isBase64Encoded
    )

  implicit val decoder: Decoder[ApplicationLoadBalancerRequestEvent] =
    Decoder.forProduct9(
      "requestContext",
      "httpMethod",
      "path",
      "queryStringParameters",
      "headers",
      "multiValueQueryStringParameters",
      "multiValueHeaders",
      "body",
      "isBase64Encoded"
    )(ApplicationLoadBalancerRequestEvent.apply)

  private final case class Impl(
      requestContext: ApplicationLoadBalancerRequestContext,
      httpMethod: String,
      path: String,
      queryStringParameters: Option[Map[String, String]],
      headers: Option[Map[CIString, String]],
      multiValueQueryStringParameters: Option[Map[String, List[String]]],
      multiValueHeaders: Option[Map[CIString, List[String]]],
      body: Option[String],
      isBase64Encoded: Boolean
  ) extends ApplicationLoadBalancerRequestEvent
}
