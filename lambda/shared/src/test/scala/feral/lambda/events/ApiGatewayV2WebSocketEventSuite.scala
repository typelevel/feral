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

class ApiGatewayV2WebSocketEventSuite extends FunSuite {

  import ApiGatewayV2WebSocketEventSuite._

  test("decode connect") {
    connectEvent.as[ApiGatewayV2WebSocketEvent].toTry.get
  }

  test("decode disconnect") {
    disconnectEvent.as[ApiGatewayV2WebSocketEvent].toTry.get
  }

}

object ApiGatewayV2WebSocketEventSuite {

  def connectEvent = json"""
  {
  "headers": {
    "Host": "abcd123.execute-api.us-east-1.amazonaws.com",
    "Sec-WebSocket-Extensions": "permessage-deflate; client_max_window_bits",
    "Sec-WebSocket-Key": "...",
    "Sec-WebSocket-Version": "13",
    "X-Amzn-Trace-Id": "...",
    "X-Forwarded-For": "192.0.2.1",
    "X-Forwarded-Port": "443",
    "X-Forwarded-Proto": "https"
  },
  "multiValueHeaders": {
    "Host": [
      "abcd123.execute-api.us-east-1.amazonaws.com"
    ],
    "Sec-WebSocket-Extensions": [
      "permessage-deflate; client_max_window_bits"
    ],
    "Sec-WebSocket-Key": [
      "..."
    ],
    "Sec-WebSocket-Version": [
      "13"
    ],
    "X-Amzn-Trace-Id": [
      "..."
    ],
    "X-Forwarded-For": [
      "192.0.2.1"
    ],
    "X-Forwarded-Port": [
      "443"
    ],
    "X-Forwarded-Proto": [
      "https"
    ]
  },
  "requestContext": {
    "routeKey": "$$connect",
    "eventType": "CONNECT",
    "extendedRequestId": "ABCD1234=",
    "requestTime": "09/Feb/2024:18:11:43 +0000",
    "messageDirection": "IN",
    "stage": "prod",
    "connectedAt": 1707502303419,
    "requestTimeEpoch": 1707502303420,
    "identity": {
      "sourceIp": "192.0.2.1"
    },
    "requestId": "ABCD1234=",
    "domainName": "abcd1234.execute-api.us-east-1.amazonaws.com",
    "connectionId": "AAAA1234=",
    "apiId": "abcd1234"
  },
  "isBase64Encoded": false
}
  """
  def disconnectEvent = json"""
  {
  "headers": {
    "Host": "abcd1234.execute-api.us-east-1.amazonaws.com",
    "x-api-key": "",
    "X-Forwarded-For": "",
    "x-restapi": ""
  },
  "multiValueHeaders": {
    "Host": [
      "abcd1234.execute-api.us-east-1.amazonaws.com"
    ],
    "x-api-key": [
      ""
    ],
    "X-Forwarded-For": [
      ""
    ],
    "x-restapi": [
      ""
    ]
  },
  "requestContext": {
    "routeKey": "$$disconnect",
    "disconnectStatusCode": 1005,
    "eventType": "DISCONNECT",
    "extendedRequestId": "ABCD1234=",
    "requestTime": "09/Feb/2024:18:23:28 +0000",
    "messageDirection": "IN",
    "disconnectReason": "Client-side close frame status not set",
    "stage": "prod",
    "connectedAt": 1707503007396,
    "requestTimeEpoch": 1707503008941,
    "identity": {
      "sourceIp": "192.0.2.1"
    },
    "requestId": "ABCD1234=",
    "domainName": "abcd1234.execute-api.us-east-1.amazonaws.com",
    "connectionId": "AAAA1234=",
    "apiId": "abcd1234"
  },
  "isBase64Encoded": false
}
    """
}

