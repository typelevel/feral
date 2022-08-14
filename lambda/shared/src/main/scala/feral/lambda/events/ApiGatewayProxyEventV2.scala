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

sealed abstract case class Http private (method: String)
object Http {
  private[lambda] def apply(method: String): Http = new Http(method) {}
  private[lambda] def unapply(http: Http): Nothing = ???

  implicit val decoder: Decoder[Http] = Decoder.forProduct1("method")(Http.apply)
}

sealed abstract case class RequestContext private (http: Http)
object RequestContext {
  private[lambda] def apply(http: Http): RequestContext = new RequestContext(http) {}
  private[lambda] def unapply(requestContext: RequestContext): Nothing = ???

  implicit val decoder: Decoder[RequestContext] =
    Decoder.forProduct1("http")(RequestContext.apply)
}

sealed abstract case class ApiGatewayProxyEventV2 private (
    rawPath: String,
    rawQueryString: String,
    cookies: Option[List[String]],
    headers: Map[String, String],
    requestContext: RequestContext,
    body: Option[String],
    isBase64Encoded: Boolean
)

object ApiGatewayProxyEventV2 {
  private[lambda] def apply(
      rawPath: String,
      rawQueryString: String,
      cookies: Option[List[String]],
      headers: Map[String, String],
      requestContext: RequestContext,
      body: Option[String],
      isBase64Encoded: Boolean
  ): ApiGatewayProxyEventV2 =
    new ApiGatewayProxyEventV2(
      rawPath,
      rawQueryString,
      cookies,
      headers,
      requestContext,
      body,
      isBase64Encoded
    ) {}

  private[lambda] def unapply(event: ApiGatewayProxyEventV2): Nothing = ???

  implicit def decoder: Decoder[ApiGatewayProxyEventV2] = Decoder.forProduct7(
    "rawPath",
    "rawQueryString",
    "cookies",
    "headers",
    "requestContext",
    "body",
    "isBase64Encoded"
  )(ApiGatewayProxyEventV2.apply)

  implicit def kernelSource: KernelSource[ApiGatewayProxyEventV2] = e => Kernel(e.headers)
}
