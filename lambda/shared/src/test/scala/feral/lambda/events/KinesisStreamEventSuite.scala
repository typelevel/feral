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

@deprecated(
  "Moved to kinesis4cats. See https://etspaceman.github.io/kinesis4cats/feral/getting-started.html.",
  since = "0.3.0")
class KinesisStreamEventSuite extends FunSuite {

  test("decoder") {
    event.as[KinesisStreamEvent].toTry.get
  }

  def event = json"""
  {
    "Records": [
      {
        "kinesis": {
          "kinesisSchemaVersion": "1.0",
          "partitionKey": "1",
          "sequenceNumber": "49590338271490256608559692538361571095921575989136588898",
          "data": "SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==",
          "approximateArrivalTimestamp": 1545084650.987
        },
        "eventSource": "aws:kinesis",
        "eventVersion": "1.0",
        "eventID": "shardId-000000000006:49590338271490256608559692538361571095921575989136588898",
        "eventName": "aws:kinesis:record",
        "invokeIdentityArn": "arn:aws:iam::123456789012:role/lambda-kinesis-role",
        "awsRegion": "us-east-2",
        "eventSourceARN": "arn:aws:kinesis:us-east-2:123456789012:stream/lambda-stream"
      }
    ]
  }
  """

}
