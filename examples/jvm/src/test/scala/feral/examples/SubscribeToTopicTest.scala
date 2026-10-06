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

package feral.examples

import feral.examples.util.TestLogHandler
import io.circe.literal._
import io.cloudevents.core.builder.CloudEventBuilder

import java.util.Base64
import java.util.logging.Logger

class SubscribeToTopicTest extends munit.FunSuite {
  val logger = Logger.getLogger("feral.examples.SubscribeToTopic")
  val testLogHandler = TestLogHandler()
  override def beforeAll(): Unit = {
    logger.addHandler(testLogHandler)
  }
  // the test uses plain json and base 64 to construct the encodedData unlike Protobuf example in StackDriverLoggingTests
  test("Google Cloud Function SubscribeToTopic should print pubsub message") {
    val msg = "Hello World"
    val encodedMessage = Base64.getEncoder().encodeToString(msg.getBytes())

    val encodedData =
      s"""{
         |"message": {
         |    "attributes": {
         |      "attr1": "attr1-value"
         |    },
         |    "data": "$encodedMessage",
         |    "messageId": "message-id",
         |    "publishTime": "2021-02-05T04:06:14.109Z"
         |  }
         |}""".stripMargin

    val event = CloudEventBuilder
      .v1()
      .withId("1234-5678-9012-3456")
      .withType("pubsub.message")
      .withSource(java.net.URI.create("https://github.com/cloudevents/spec/pull/123"))
      .withData(encodedData.getBytes())
      .build()

    new SubscribeToTopic().accept(event)

    val messages = testLogHandler.getLog

    val first_mesage = messages
      .filter(r => r.getMessage().contains("data"))
      .head
      .getMessage()
      .split(":")(1)
      .trim()

    val res = json"""{
      "message": {
          "attributes": {
              "attr1":"attr1-value"
          },
          "data": "SGVsbG8gV29ybGQ=",
          "messageId": "message-id",
          "publishTime":"2021-02-05T04:06:14.109Z"
        }
      }
    """

    val data =
      res.hcursor.downField("message").downField("data").focus.flatMap(_.asString).get.trim()

    assertEquals(
      data,
      first_mesage
    )
  }
}
