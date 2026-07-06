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

class CloudWatchLogsEventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[CloudWatchLogsEvent].toTry.get, expected)
  }

  test("decoded data decoder") {
    assertEquals(decodedDataJson.as[CloudWatchLogsDecodedData].toTry.get, expectedDecodedData)
  }

  def event = json"""
  {
    "awslogs": {
      "data": "H4sIAAAAAAAA/6tWKkktLlGyUlAqSS0u0QHQKhZnJ2cm5+cXZOYnlQAAAP//"
    }
  }
  """

  def expected =
    CloudWatchLogsEvent(
      awslogs = CloudWatchLogsEventData(
        data = "H4sIAAAAAAAA/6tWKkktLlGyUlAqSS0u0QHQKhZnJ2cm5+cXZOYnlQAAAP//"
      )
    )

  def decodedDataJson = json"""
  {
    "owner": "123456789012",
    "logGroup": "/aws/lambda/my-function",
    "logStream": "2024/01/15/[$$LATEST]abc123",
    "subscriptionFilters": ["filter-1"],
    "messageType": "DATA_MESSAGE",
    "logEvents": [
      {
        "id": "event-id-1",
        "timestamp": 1705312800000,
        "message": "Log line one",
        "extractedFields": { "field1": "value1" }
      }
    ]
  }
  """

  def expectedDecodedData =
    CloudWatchLogsDecodedData(
      owner = "123456789012",
      logGroup = "/aws/lambda/my-function",
      logStream = "2024/01/15/[$LATEST]abc123",
      subscriptionFilters = List("filter-1"),
      messageType = "DATA_MESSAGE",
      logEvents = List(
        CloudWatchLogsLogEvent(
          id = "event-id-1",
          timestamp = 1705312800000L,
          message = "Log line one",
          extractedFields = Some(Map("field1" -> "value1"))
        )
      )
    )
}
