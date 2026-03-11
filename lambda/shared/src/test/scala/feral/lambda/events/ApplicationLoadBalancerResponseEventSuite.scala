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

import io.circe.literal._
import io.circe.syntax._
import munit.FunSuite
import org.typelevel.ci.CIString

class ApplicationLoadBalancerResponseEventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[ApplicationLoadBalancerResponseEvent].toTry.get, expected)
  }

  test("encoder round-trip") {
    val decoded = event.as[ApplicationLoadBalancerResponseEvent].toTry.get
    assertEquals(decoded.asJson.as[ApplicationLoadBalancerResponseEvent].toTry.get, decoded)
  }

  def event = json"""
  {
    "statusCode": 200,
    "statusDescription": "OK",
    "headers": {
      "Content-Type": "application/json"
    },
    "multiValueHeaders": {
      "Set-Cookie": ["session=abc", "path=/"]
    },
    "body": "{\"message\":\"Hello\"}",
    "isBase64Encoded": false
  }
  """

  def expected =
    ApplicationLoadBalancerResponseEvent(
      statusCode = 200,
      statusDescription = Some("OK"),
      headers = Some(Map(CIString("Content-Type") -> "application/json")),
      multiValueHeaders = Some(Map(CIString("Set-Cookie") -> List("session=abc", "path=/"))),
      body = Some("{\"message\":\"Hello\"}"),
      isBase64Encoded = Some(false)
    )
}
