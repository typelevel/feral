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

class CodeCommitEventSuite extends FunSuite {
  import CodeCommitEventSuite._

  test("decoder") {
    assertEquals(event.as[CodeCommitEvent].toTry.get, decoded)
    assertEquals(
      eventNoCustomDataWithDelete.as[CodeCommitEvent].toTry.get,
      decodedNoCustomDataWithDelete)
  }
}

object CodeCommitEventSuite {

  val decoded: CodeCommitEvent =
    CodeCommitEvent(
      List(
        CodeCommitRecord(
          "us-east-1",
          CodeCommitData(
            List(
              CodeCommitReference(
                "5e493c6f3067653f3d04eca608b4901eb227078",
                "refs/heads/main",
                Some(true),
                None
              )
            )
          ),
          Some("this is custom data"),
          "31eze9a2-1b2d-ffef-b1d1-e8ede6720eb3",
          "TriggerEventTest",
          1,
          "aws:codecommit",
          "arn:aws:codecommit:us-east-1:123456789012:MyDemoRepo",
          "2016-01-01T23:59:59.000+0000",
          1,
          "5582e977-EXAMPLE",
          "my-trigger",
          "1",
          "arn:aws:iam::123456789012:root"
        )
      )
    )

  val decodedNoCustomDataWithDelete: CodeCommitEvent =
    CodeCommitEvent(
      List(
        CodeCommitRecord(
          "us-east-1",
          CodeCommitData(
            List(
              CodeCommitReference(
                "a]b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "refs/heads/feature",
                None,
                Some(true)
              )
            )
          ),
          None,
          "42fze9a2-1b2d-ffef-b1d1-e8ede6720eb4",
          "TriggerEventTest",
          1,
          "aws:codecommit",
          "arn:aws:codecommit:us-east-1:123456789012:MyDemoRepo",
          "2016-02-01T23:59:59.000+0000",
          1,
          "6693f088-EXAMPLE",
          "my-trigger",
          "1",
          "arn:aws:iam::123456789012:root"
        )
      )
    )

  // https://docs.aws.amazon.com/lambda/latest/dg/services-codecommit.html
  val event: Json = json"""
    {
      "Records": [
        {
          "awsRegion": "us-east-1",
          "codecommit": {
            "references": [
              {
                "commit": "5e493c6f3067653f3d04eca608b4901eb227078",
                "ref": "refs/heads/main",
                "created": true
              }
            ]
          },
          "customData": "this is custom data",
          "eventId": "31eze9a2-1b2d-ffef-b1d1-e8ede6720eb3",
          "eventName": "TriggerEventTest",
          "eventPartNumber": 1,
          "eventSource": "aws:codecommit",
          "eventSourceARN": "arn:aws:codecommit:us-east-1:123456789012:MyDemoRepo",
          "eventTime": "2016-01-01T23:59:59.000+0000",
          "eventTotalParts": 1,
          "eventTriggerConfigId": "5582e977-EXAMPLE",
          "eventTriggerName": "my-trigger",
          "eventVersion": "1",
          "userIdentityARN": "arn:aws:iam::123456789012:root"
        }
      ]
    }
  """

  val eventNoCustomDataWithDelete: Json = json"""
    {
      "Records": [
        {
          "awsRegion": "us-east-1",
          "codecommit": {
            "references": [
              {
                "commit": "a]b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "ref": "refs/heads/feature",
                "deleted": true
              }
            ]
          },
          "eventId": "42fze9a2-1b2d-ffef-b1d1-e8ede6720eb4",
          "eventName": "TriggerEventTest",
          "eventPartNumber": 1,
          "eventSource": "aws:codecommit",
          "eventSourceARN": "arn:aws:codecommit:us-east-1:123456789012:MyDemoRepo",
          "eventTime": "2016-02-01T23:59:59.000+0000",
          "eventTotalParts": 1,
          "eventTriggerConfigId": "6693f088-EXAMPLE",
          "eventTriggerName": "my-trigger",
          "eventVersion": "1",
          "userIdentityARN": "arn:aws:iam::123456789012:root"
        }
      ]
    }
  """
}
