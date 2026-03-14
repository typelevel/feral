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

import feral.lambda.events.ApiGatewayCustomAuthorizerEvent
import io.circe.literal._
import munit.FunSuite

class ApiGatewayCustomAuthorizerEventSuite extends FunSuite {

  import ApiGatewayCustomAuthorizerEventSuite._

  test("decoder") {
    event.as[ApiGatewayCustomAuthorizerEvent].toTry.get
  }
}

object ApiGatewayCustomAuthorizerEventSuite {
  def event = json"""
    {
        "type": "REQUEST",
        "methodArn": "arn:aws:execute-api:us-east-1:677276103099:or71kuogm2/test/GET/",
        "resource": "/",
        "path": "/",
        "httpMethod": "ANY",
        "headers": {
            "accept": "*/*",
            "headerauth1": "headerValue1",
            "Host": "or71kuogm2.execute-api.us-east-1.amazonaws.com",
            "user-agent": "curl/7.81.0",
            "X-Amzn-Trace-Id": "Root=1-69b199a4-2c09a07109124da13b8ff7b0",
            "X-Forwarded-For": "102.176.65.68",
            "X-Forwarded-Port": "443",
            "X-Forwarded-Proto": "https"
        },
        "multiValueHeaders": {
            "accept": [
                "*/*"
            ],
            "headerauth1": [
                "headerValue1"
            ],
            "Host": [
                "or71kuogm2.execute-api.us-east-1.amazonaws.com"
            ],
            "user-agent": [
                "curl/7.81.0"
            ],
            "X-Amzn-Trace-Id": [
                "Root=1-69b199a4-2c09a07109124da13b8ff7b0"
            ],
            "X-Forwarded-For": [
                "102.176.65.68"
            ],
            "X-Forwarded-Port": [
                "443"
            ],
            "X-Forwarded-Proto": [
                "https"
            ]
        },
        "queryStringParameters": {
            "QueryString1": "queryValue1"
        },
        "multiValueQueryStringParameters": {
            "QueryString1": [
                "queryValue1"
            ]
        },
        "pathParameters": {},
        "stageVariables": {
            "StageVar1": "stageValue1"
        },
        "requestContext": {
            "resourceId": "8qkclnaedk",
            "resourcePath": "/",
            "httpMethod": "GET",
            "extendedRequestId": "aETx1HJcIAMEGdw=",
            "requestTime": "11/Mar/2026:16:34:44 +0000",
            "path": "/test",
            "accountId": "677276103099",
            "protocol": "HTTP/1.1",
            "stage": "test",
            "domainPrefix": "or71kuogm2",
            "requestTimeEpoch": 1773246884924,
            "requestId": "b20e5103-7f01-4bef-9fa2-682889187b5b",
            "identity": {
                "cognitoIdentityPoolId": null,
                "accountId": null,
                "cognitoIdentityId": null,
                "caller": null,
                "sourceIp": "102.176.65.68",
                "principalOrgId": null,
                "accessKey": null,
                "cognitoAuthenticationType": null,
                "cognitoAuthenticationProvider": null,
                "userArn": null,
                "userAgent": "curl/7.81.0",
                "user": null
            },
            "domainName": "or71kuogm2.execute-api.us-east-1.amazonaws.com",
            "deploymentId": "ltoy3m",
            "apiId": "or71kuogm2"
        }
    }

    """
}
