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
import scodec.bits.ByteVector

import java.time.Instant
import java.util.UUID

class SnsEventSuite extends FunSuite {
  import SnsEventSuite._

  test("decoder") {
    assertEquals(event.as[SnsEvent].toTry.get, decoded)
  }
}

object SnsEventSuite {
  val decoded: SnsEvent =
    SnsEvent(
      List(
        SnsEventRecord(
          "1.0",
          "arn:aws:sns:TEST",
          "aws:sns",
          SnsMessage(
            "TEST",
            UUID.fromString("9f0cfa95-e344-4f93-89ab-5e979503ed1f"),
            "Notification",
            "arn:aws:sns:TEST",
            Map(
              "number" -> SnsMessageAttribute.Number(1e+6),
              "binary" -> SnsMessageAttribute.Binary(ByteVector.fromBase64("VGVzdGluZwo=").get),
              "string" -> SnsMessageAttribute.String("Testing"),
              "numberString" -> SnsMessageAttribute.Number(5),
              "array" -> SnsMessageAttribute.StringArray(List(
                SnsMessageAttributeArrayMember.Number(1),
                SnsMessageAttributeArrayMember.String("two"),
                SnsMessageAttributeArrayMember.Boolean(true),
                SnsMessageAttributeArrayMember.Boolean(false)
              )),
              "unsupported" -> SnsMessageAttribute
                .Unknown("FancyNewType", Some("SpecialValueHere"))
            ),
            "1",
            Instant.parse("2022-04-06T01:02:03.456Z"),
            "TEST",
            "Testing Message",
            "TEST",
            "Test Message"
          )
        )
      )
    )

  val event: Json = json"""
    {
      "Records": [
        {
          "EventVersion": "1.0",
          "EventSubscriptionArn": "arn:aws:sns:TEST",
          "EventSource": "aws:sns",
          "Sns": {
            "Signature": "TEST",
            "MessageId": "9f0cfa95-e344-4f93-89ab-5e979503ed1f",
            "Type": "Notification",
            "TopicArn": "arn:aws:sns:TEST",
            "MessageAttributes": {
              "string": {
                "Type": "String",
                "Value": "Testing"
              },
              "binary": {
                "Type": "Binary",
                "Value": "VGVzdGluZwo="
              },
              "number": {
                "Type": "Number",
                "Value": 1e6
              },
              "numberString": {
                "Type": "Number",
                "Value": "5"
              },
              "array": {
                "Type": "String.Array",
                "Value": "[1, \"two\", true, false]"
              },
              "unsupported": {
                "Type": "FancyNewType",
                "Value": "SpecialValueHere"
              }
            },
            "SignatureVersion": "1",
            "Timestamp": "2022-04-06T01:02:03.456Z",
            "SigningCertUrl": "TEST",
            "Message": "Testing Message",
            "UnsubscribeUrl": "TEST",
            "Subject": "Test Message"
          }
        }
      ]
    }
  """
}
