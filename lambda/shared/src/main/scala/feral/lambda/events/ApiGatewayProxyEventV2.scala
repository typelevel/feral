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

sealed abstract class Http {
  def method: String
}

object Http {
  def apply(method: String): Http =
    new Impl(method)

  private[events] implicit val decoder: Decoder[Http] =
    Decoder.forProduct1("method")(Http.apply)

  private final case class Impl(method: String) extends Http {
    override def productPrefix = "Http"
  }
}

sealed abstract class RequestContext {
  def http: Http
}

object RequestContext {
  def apply(http: Http): RequestContext =
    new Impl(http)

  private[events] implicit val decoder: Decoder[RequestContext] =
    Decoder.forProduct1("http")(RequestContext.apply)

  final case class Impl(http: Http) extends RequestContext {
    override def productPrefix = "RequestContext"
  }
}

sealed abstract class ApiGatewayProxyEventV2 {
  def rawPath: String
  def rawQueryString: String
  def cookies: Option[List[String]]
  def headers: Map[CIString, String]
  def requestContext: RequestContext
  def body: Option[String]
  def isBase64Encoded: Boolean
}

object ApiGatewayProxyEventV2 {
  def apply(
      rawPath: String,
      rawQueryString: String,
      cookies: Option[List[String]],
      headers: Map[CIString, String],
      requestContext: RequestContext,
      body: Option[String],
      isBase64Encoded: Boolean
  ): ApiGatewayProxyEventV2 =
    new Impl(rawPath, rawQueryString, cookies, headers, requestContext, body, isBase64Encoded)

  import codecs.decodeKeyCIString
  implicit def decoder: Decoder[ApiGatewayProxyEventV2] = Decoder.forProduct7(
    "rawPath",
    "rawQueryString",
    "cookies",
    "headers",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(ApiGatewayProxyEventV2.apply)

  implicit def kernelSource: KernelSource[ApiGatewayProxyEventV2] =
    e => Kernel(e.headers)

  private final case class Impl(
      rawPath: String,
      rawQueryString: String,
      cookies: Option[List[String]],
      headers: Map[CIString, String],
      requestContext: RequestContext,
      body: Option[String],
      isBase64Encoded: Boolean
  ) extends ApiGatewayProxyEventV2 {
    override def productPrefix = "ApiGatewayProxyEventV2"
  }
}
