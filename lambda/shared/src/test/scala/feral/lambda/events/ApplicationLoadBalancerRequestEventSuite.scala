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
    allFieldsEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
  }

  test("decode with only body present") {
    withBodyEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
  }

  test("decode with only required fields") {
    missingOptionalsEvent.as[ApplicationLoadBalancerRequestEvent].toTry.get
  }
}

object ApplicationLoadBalancerRequestEventSuite {
  import io.circe.literal._

  def allFieldsEvent = json"""
    {
      "requestContext": {
        "elb": {
          "targetGroupArn": "arn:aws:elasticloadbalancing:region:account-id:targetgroup/target-group-name/1234567890abcdef"
        }
      },
      "httpMethod": "GET",
      "path": "/my/path",
      "queryStringParameters": {"foo": "bar"},
      "headers": {"host": "example.com"},
      "multiValueQueryStringParameters": {"foo": ["bar"]},
      "multiValueHeaders": {"host": ["example.com"]},
      "body": null,
      "isBase64Encoded": false
    }
  """

  def withBodyEvent = json"""
    {
      "requestContext": {
        "elb": {
          "targetGroupArn": "arn:aws:elasticloadbalancing:region:account-id:targetgroup/target-group-name/1234567890abcdef"
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
          "targetGroupArn": "arn:aws:elasticloadbalancing:region:account-id:targetgroup/target-group-name/1234567890abcdef"
        }
      },
      "httpMethod": "GET",
      "path": "/only-required",
      "isBase64Encoded": false
    }
  """
}
