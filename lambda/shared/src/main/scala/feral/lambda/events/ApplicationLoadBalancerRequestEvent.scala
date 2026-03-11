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
import org.typelevel.ci.CIString

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/alb.d.ts
// https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html

sealed abstract class ApplicationLoadBalancerRequestContext {
  def elb: ApplicationLoadBalancerTargetGroup
}

object ApplicationLoadBalancerRequestContext {

  def apply(elb: ApplicationLoadBalancerTargetGroup): ApplicationLoadBalancerRequestContext =
    new Impl(elb)

  private[events] implicit val decoder: Decoder[ApplicationLoadBalancerRequestContext] =
    Decoder.forProduct1("elb")(ApplicationLoadBalancerRequestContext.apply)

  private final case class Impl(elb: ApplicationLoadBalancerTargetGroup)
      extends ApplicationLoadBalancerRequestContext {
    override def productPrefix = "ApplicationLoadBalancerRequestContext"
  }
}

sealed abstract class ApplicationLoadBalancerTargetGroup {
  def targetGroupArn: String
}

object ApplicationLoadBalancerTargetGroup {

  def apply(targetGroupArn: String): ApplicationLoadBalancerTargetGroup =
    new Impl(targetGroupArn)

  private[events] implicit val decoder: Decoder[ApplicationLoadBalancerTargetGroup] =
    Decoder.forProduct1("targetGroupArn")(ApplicationLoadBalancerTargetGroup.apply)

  private final case class Impl(targetGroupArn: String)
      extends ApplicationLoadBalancerTargetGroup {
    override def productPrefix = "ApplicationLoadBalancerTargetGroup"
  }
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
}

object ApplicationLoadBalancerRequestEvent {

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
    new Impl(
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

  import codecs.decodeKeyCIString
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
  ) extends ApplicationLoadBalancerRequestEvent {
    override def productPrefix = "ApplicationLoadBalancerRequestEvent"
  }
}
