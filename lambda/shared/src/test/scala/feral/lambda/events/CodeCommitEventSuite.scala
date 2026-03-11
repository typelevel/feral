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

class CodeCommitEventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[CodeCommitEvent].toTry.get, expected)
  }

  def event = json"""
  {
    "Records": [
      {
        "awsRegion": "us-east-1",
        "codecommit": {
          "references": [
            {
              "commit": "4c925148dc8d2c6a6d7c2f8b9a1b2c3d4e5f6a7b",
              "created": true,
              "deleted": false,
              "ref": "refs/heads/main"
            }
          ]
        },
        "customData": "optional-data",
        "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "eventName": "ReferenceChanges",
        "eventPartNumber": 1,
        "eventSource": "aws:codecommit",
        "eventSourceARN": "arn:aws:codecommit:us-east-1:123456789012:my-repo",
        "eventTime": "2024-01-15T12:00:00.000Z",
        "eventTotalParts": 1,
        "eventTriggerConfigId": "trigger-123",
        "eventTriggerName": "my-trigger",
        "eventVersion": "1.0",
        "userIdentityARN": "arn:aws:iam::123456789012:user/Alice"
      }
    ]
  }
  """

  def expected =
    CodeCommitEvent(
      records = List(
        CodeCommitTrigger(
          awsRegion = "us-east-1",
          codecommit = CodeCommitReferences(
            references = List(
              CodeCommitReference(
                commit = "4c925148dc8d2c6a6d7c2f8b9a1b2c3d4e5f6a7b",
                created = Some(true),
                deleted = Some(false),
                ref = "refs/heads/main"
              )
            )
          ),
          customData = Some("optional-data"),
          eventId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          eventName = "ReferenceChanges",
          eventPartNumber = 1,
          eventSource = "aws:codecommit",
          eventSourceArn = "arn:aws:codecommit:us-east-1:123456789012:my-repo",
          eventTime = "2024-01-15T12:00:00.000Z",
          eventTotalParts = 1,
          eventTriggerConfigId = "trigger-123",
          eventTriggerName = "my-trigger",
          eventVersion = "1.0",
          userIdentityArn = "arn:aws:iam::123456789012:user/Alice"
        )
      )
    )
}
