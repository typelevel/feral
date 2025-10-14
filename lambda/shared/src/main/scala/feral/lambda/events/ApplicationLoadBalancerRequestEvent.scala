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

package feral.lambda.events

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

final case class ApplicationLoadBalancerRequestContext(
                                                        elb: ApplicationLoadBalancerRequestContext.Elb
                                                      )
object ApplicationLoadBalancerRequestContext {
  final case class Elb(targetGroupArn: String)
  implicit val elbDecoder: Decoder[Elb] = deriveDecoder
  implicit val elbEncoder: Encoder[Elb] = deriveEncoder

  implicit val decoder: Decoder[ApplicationLoadBalancerRequestContext] = deriveDecoder
  implicit val encoder: Encoder[ApplicationLoadBalancerRequestContext] = deriveEncoder
}

final case class ApplicationLoadBalancerRequestEvent(
                                                      requestContext: ApplicationLoadBalancerRequestContext,
                                                      httpMethod: String,
                                                      path: String,
                                                      queryStringParameters: Option[Map[String, Option[String]]],
                                                      headers: Option[Map[String, Option[String]]],
                                                      multiValueQueryStringParameters: Option[Map[String, Option[List[String]]]],
                                                      multiValueHeaders: Option[Map[String, Option[List[String]]]],
                                                      body: Option[String], // because can be null
                                                      isBase64Encoded: Boolean
                                                    )

object ApplicationLoadBalancerRequestEvent {
  implicit val decoder: Decoder[ApplicationLoadBalancerRequestEvent] = deriveDecoder
  implicit val encoder: Encoder[ApplicationLoadBalancerRequestEvent] = deriveEncoder
}