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

class SQSEventSuite extends FunSuite {

  test("decoder") {
    event.as[SQSEvent].toTry.get
  }

  def event = json"""
  {
    "Records": [
      {
        "messageId": "059f36b4-87a3-44ab-83d2-661975830a7d",
        "receiptHandle": "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
        "body": "test",
        "attributes": {
          "ApproximateReceiveCount": "1",
          "SentTimestamp": "1545082649183",
          "SenderId": "AIDAIENQZJOLO23YVJ4VO",
          "ApproximateFirstReceiveTimestamp": "1545082649185"
        },
        "messageAttributes": {
        },
        "md5OfBody": "098f6bcd4621d373cade4e832627b4f6",
        "eventSource": "aws:sqs",
        "eventSourceARN": "arn:aws:sqs:us-east-2:123456789012:my-queue",
        "awsRegion": "us-east-2"
      }
    ]
  }
  """

}
