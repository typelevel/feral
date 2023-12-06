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

package feral.lambda.http4s

import cats.effect.IO
import cats.syntax.all._
import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.ApiGatewayProxyEventSuite.*
import munit.CatsEffectSuite
import org.http4s.Headers
import org.http4s.Method
import org.http4s.syntax.all._

class ApiGatewayProxyHandlerV1Suite extends CatsEffectSuite {

  val expectedHeaders: Headers = Headers(
    "Accept-Language" -> "en-US,en;q=0.8",
    "CloudFront-Is-Mobile-Viewer" -> "false",
    "CloudFront-Is-Desktop-Viewer" -> "true",
    "Via" -> "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)",
    "X-Amz-Cf-Id" -> "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==",
    "Host" -> "1234567890.execute-api.us-east-1.amazonaws.com",
    "Accept-Encoding" -> "gzip, deflate, sdch",
    "X-Forwarded-Port" -> "443",
    "Cache-Control" -> "max-age=0",
    "CloudFront-Viewer-Country" -> "US",
    "CloudFront-Is-SmartTV-Viewer" -> "false",
    "X-Forwarded-Proto" -> "https",
    "Upgrade-Insecure-Requests" -> "1",
    "User-Agent" -> "Custom User Agent String",
    "CloudFront-Forwarded-Proto" -> "https",
    "X-Forwarded-For" -> "127.0.0.1, 127.0.0.2",
    "CloudFront-Is-Tablet-Viewer" -> "false",
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
  )

  val expectedBody: String = """{"test":"body"}"""

  test("decode event") {
    for {
      event <- event.as[ApiGatewayProxyEvent].liftTo[IO]
      request <- ApiGatewayProxyHandlerV1.decodeEvent[IO](event)
      _ <- IO(assertEquals(request.method, Method.POST))
      _ <- IO(assertEquals(request.uri, uri"/path/to/resource?foo=bar&foo=bar"))
      _ <- IO(assertEquals(request.headers, expectedHeaders))
      responseBody <- request.bodyText.compile.string
      _ <- IO(assertEquals(responseBody, expectedBody))
    } yield ()
  }

}
