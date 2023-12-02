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
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.ApiGatewayProxyEventV2Suite
import munit.CatsEffectSuite
import org.http4s.Headers
import org.http4s.Method
import org.http4s.syntax.all._

class ApiGatewayProxyHandlerV2Suite extends CatsEffectSuite {

  import ApiGatewayProxyEventV2Suite._

  test("decode event") {
    for {
      event <- event.as[ApiGatewayProxyEventV2].liftTo[IO]
      request <- ApiGatewayProxyHandlerV2.decodeEvent[IO](event)
      _ <- IO(assertEquals(request.method, Method.GET))
      _ <- IO(assertEquals(request.uri, uri"/default/nodejs-apig-function-1G3XMPLZXVXYI?"))
      _ <- IO(assert(request.cookies.nonEmpty))
      _ <- IO(assertEquals(request.headers, expectedHeaders))
      _ <- request.body.compile.count.assertEquals(0L)
    } yield ()
  }

  test("decode event with no cookies") {
    for {
      event <- eventNoCookies.as[ApiGatewayProxyEventV2].liftTo[IO]
      request <- ApiGatewayProxyHandlerV2.decodeEvent[IO](event)
      _ <- IO(assert(request.cookies.isEmpty))
      _ <- request.body.compile.count.assertEquals(0L)
    } yield ()
  }

  def expectedHeaders = Headers(
    "content-length" -> "0",
    "accept-language" -> "en-US,en;q=0.9",
    "sec-fetch-dest" -> "document",
    "sec-fetch-user" -> "?1",
    "x-amzn-trace-id" -> "Root=1-5e6722a7-cc56xmpl46db7ae02d4da47e",
    "host" -> "r3pmxmplak.execute-api.us-east-2.amazonaws.com",
    "sec-fetch-mode" -> "navigate",
    "accept-encoding" -> "gzip, deflate, br",
    "accept" ->
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
    "sec-fetch-site" -> "cross-site",
    "x-forwarded-port" -> "443",
    "x-forwarded-proto" -> "https",
    "upgrade-insecure-requests" -> "1",
    "user-agent" ->
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36",
    "x-forwarded-for" -> "205.255.255.176",
    "cookie" -> "s_fid=7AABXMPL1AFD9BBF-0643XMPL09956DE2; regStatus=pre-register"
  )

}
