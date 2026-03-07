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

import io.circe.Json
import io.circe.literal._
import munit.FunSuite

import java.time.Instant

class CloudWatchLogsEventSuite extends FunSuite {
  import CloudWatchLogsEventSuite._

  test("event decoder") {
    assertEquals(event.as[CloudWatchLogsEvent].toTry.get, decodedEvent)
  }

  test("decoded data decoder") {
    assertEquals(decodedDataJson.as[CloudWatchLogsDecodedData].toTry.get, decodedData)
  }
}

object CloudWatchLogsEventSuite {

  val decodedEvent: CloudWatchLogsEvent =
    CloudWatchLogsEvent(
      CloudWatchLogsEventData(
        "H4sIAAAAAAAAE6tWKkktLlGyUlAqS8wpTgUAKLMMdBMAAAA="
      )
    )

  val decodedData: CloudWatchLogsDecodedData =
    CloudWatchLogsDecodedData(
      "123456789012",
      "testLogGroup",
      "testLogStream",
      List("testFilter"),
      "DATA_MESSAGE",
      List(
        CloudWatchLogsLogEvent(
          "eventId1",
          Instant.ofEpochMilli(1440442987000L),
          "[ERROR] First test message",
          None
        ),
        CloudWatchLogsLogEvent(
          "eventId2",
          Instant.ofEpochMilli(1440442987001L),
          "[ERROR] Second test message",
          None
        )
      )
    )

  // https://docs.aws.amazon.com/lambda/latest/dg/services-cloudwatchlogs.html
  val event: Json = json"""
    {
      "awslogs": {
        "data": "H4sIAAAAAAAAE6tWKkktLlGyUlAqS8wpTgUAKLMMdBMAAAA="
      }
    }
  """

  // https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html
  val decodedDataJson: Json = json"""
    {
      "owner": "123456789012",
      "logGroup": "testLogGroup",
      "logStream": "testLogStream",
      "subscriptionFilters": ["testFilter"],
      "messageType": "DATA_MESSAGE",
      "logEvents": [
        {
          "id": "eventId1",
          "timestamp": 1440442987000,
          "message": "[ERROR] First test message"
        },
        {
          "id": "eventId2",
          "timestamp": 1440442987001,
          "message": "[ERROR] Second test message"
        }
      ]
    }
  """
}
