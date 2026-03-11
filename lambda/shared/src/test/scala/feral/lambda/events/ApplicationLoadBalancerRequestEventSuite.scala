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
import munit.FunSuite
import org.typelevel.ci.CIString

class ApplicationLoadBalancerRequestEventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[ApplicationLoadBalancerRequestEvent].toTry.get, expected)
  }

  def event = json"""
  {
    "requestContext": {
      "elb": {
        "targetGroupArn": "arn:aws:elasticloadbalancing:us-east-2:123456789012:targetgroup/lambda-target/1234567890abcdef"
      }
    },
    "httpMethod": "GET",
    "path": "/lambda",
    "queryStringParameters": {
      "foo": "bar"
    },
    "headers": {
      "accept": "text/html,application/xhtml+xml",
      "host": "lambda-1234567890.us-east-2.elb.amazonaws.com"
    },
    "multiValueQueryStringParameters": {
      "foo": ["bar", "baz"]
    },
    "multiValueHeaders": {
      "accept": ["text/html", "application/xhtml+xml"],
      "host": ["lambda-1234567890.us-east-2.elb.amazonaws.com"]
    },
    "body": null,
    "isBase64Encoded": false
  }
  """

  def expected =
    ApplicationLoadBalancerRequestEvent(
      requestContext = ApplicationLoadBalancerRequestContext(
        ApplicationLoadBalancerTargetGroup(
          "arn:aws:elasticloadbalancing:us-east-2:123456789012:targetgroup/lambda-target/1234567890abcdef"
        )
      ),
      httpMethod = "GET",
      path = "/lambda",
      queryStringParameters = Some(Map("foo" -> "bar")),
      headers = Some(
        Map(
          CIString("accept") -> "text/html,application/xhtml+xml",
          CIString("host") -> "lambda-1234567890.us-east-2.elb.amazonaws.com"
        )
      ),
      multiValueQueryStringParameters = Some(Map("foo" -> List("bar", "baz"))),
      multiValueHeaders = Some(
        Map(
          CIString("accept") -> List("text/html", "application/xhtml+xml"),
          CIString("host") -> List("lambda-1234567890.us-east-2.elb.amazonaws.com")
        )
      ),
      body = None,
      isBase64Encoded = false
    )
}
