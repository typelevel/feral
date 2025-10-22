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

import munit.FunSuite

class ApplicationLoadBalancerRequestEventSuite extends FunSuite {
  import ApplicationLoadBalancerRequestEventSuite._

  test("decode with all fields populated") {
    val decoded = allFieldsEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
    val expected = ApplicationLoadBalancerRequestEvent(
      requestContext = ApplicationLoadBalancerRequestContext(
        elb = Elb("arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09")
      ),
      httpMethod = "GET",
      path = "/lambda",
      queryStringParameters = Some(Map("query" -> "1234ABCD")),
      headers = Some(Map(
        org.typelevel.ci.CIString("accept") -> "text/html,application/xhtml+xml",
        org.typelevel.ci.CIString("accept-language") -> "en-US,en;q=0.8",
        org.typelevel.ci.CIString("content-type") -> "text/plain",
        org.typelevel.ci.CIString("cookie") -> "cookies",
        org.typelevel.ci.CIString("host") -> "lambda-846800462-us-west-2.elb.amazonaws.com",
        org.typelevel.ci.CIString("user-agent") -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6)",
        org.typelevel.ci.CIString("x-amzn-trace-id") -> "Root=1-58337364-23a8c76965a2ef7629b2b5c2",
        org.typelevel.ci.CIString("x-forwarded-for") -> "72.21.198.64",
        org.typelevel.ci.CIString("x-forwarded-port") -> "443",
        org.typelevel.ci.CIString("x-forwarded-proto") -> "https"
      )),
      multiValueQueryStringParameters = None,
      multiValueHeaders = None,
      body = None,
      isBase64Encoded = false
    )
    assertEquals(decoded, expected)
  }

  test("decode with only body present") {
    val decoded = withBodyEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
    val expected = ApplicationLoadBalancerRequestEvent(
      requestContext = ApplicationLoadBalancerRequestContext(
        elb = Elb("arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09")
      ),
      httpMethod = "POST",
      path = "/submit",
      queryStringParameters = None,
      headers = None,
      multiValueQueryStringParameters = None,
      multiValueHeaders = None,
      body = Some("hello world"),
      isBase64Encoded = true
    )
    assertEquals(decoded, expected)
  }

  test("decode with only required fields") {
    val decoded = missingOptionalsEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
    val expected = ApplicationLoadBalancerRequestEvent(
      requestContext = ApplicationLoadBalancerRequestContext(
        elb = Elb("arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09")
      ),
      httpMethod = "GET",
      path = "/only-required",
      queryStringParameters = None,
      headers = None,
      multiValueQueryStringParameters = None,
      multiValueHeaders = None,
      body = None,
      isBase64Encoded = false
    )
    assertEquals(decoded, expected)
  }
}

object ApplicationLoadBalancerRequestEventSuite {
  import io.circe.literal._

  def withBodyEvent = json"""
    {
      "requestContext": {
        "elb": {
          "targetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09"
        }
      },
      "httpMethod": "POST",
      "path": "/submit",
      "queryStringParameters": null,
      "headers": null,
      "multiValueQueryStringParameters": null,
      "multiValueHeaders": null,
      "body": "hello world",
      "isBase64Encoded": true
    }
  """

  def missingOptionalsEvent = json"""
    {
      "requestContext": {
        "elb": {
          "targetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09"
        }
      },
      "httpMethod": "GET",
      "path": "/only-required",
      "isBase64Encoded": false
    }
  """

  def allFieldsEvent = json"""
    {
      "requestContext": {
        "elb": {
          "targetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:123456789012:targetgroup/my-target-group/6d0ecf831eec9f09"
        }
      },
      "httpMethod": "GET",
      "path": "/lambda",
      "queryStringParameters": {
        "query": "1234ABCD"
      },
      "headers": {
        "accept": "text/html,application/xhtml+xml",
        "accept-language": "en-US,en;q=0.8",
        "content-type": "text/plain",
        "cookie": "cookies",
        "host": "lambda-846800462-us-west-2.elb.amazonaws.com",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6)",
        "x-amzn-trace-id": "Root=1-58337364-23a8c76965a2ef7629b2b5c2",
        "x-forwarded-for": "72.21.198.64",
        "x-forwarded-port": "443",
        "x-forwarded-proto": "https"
      },
      "isBase64Encoded": false
    }
  """
}
