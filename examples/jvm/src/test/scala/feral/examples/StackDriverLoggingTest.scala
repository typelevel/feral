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

import com.google.events.cloud.pubsub.v1.MessagePublishedData
import com.google.events.cloud.pubsub.v1.PubsubMessage
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import feral.examples.util.TestLogHandler
import io.cloudevents.core.builder.CloudEventBuilder

import java.util.logging.Logger

class StackDriverLoggingTest extends munit.FunSuite {
  val logger = Logger.getLogger("feral.examples.StackDriverLogging")
  val testLogHandler = TestLogHandler()
  override def beforeAll(): Unit = {
    logger.setUseParentHandlers(false)
    logger.addHandler(testLogHandler)
  }

  override def afterEach(context: AfterEach): Unit =
    testLogHandler.clear()

  test("Message sent over the wire should output \'hello name\' where name is Feral") {
    val msg = "Feral"

    // The message variable will spit out a protobuf format and converted to
    // json format to produce the below
    // "message": {
    //     "attributes": {
    //       "attr1": "attr1-value"
    //     },
    //     "data": "Feral",
    //     "messageId": "message-id",
    //     "publishTime": "2021-02-05T04:06:14.109Z"
    //   }

    val message = PubsubMessage
      .newBuilder()
      .setData(ByteString.copyFromUtf8(msg))
      .putAttributes("attr1", "attr1-value")
      .setMessageId("message-id")
      .setPublishTime(Timestamps.parse("2021-02-05T04:06:14.109Z"))
      .build()

    val data = MessagePublishedData.newBuilder().setMessage(message).build()

    val json_formatted_data = JsonFormat.printer().print(data)

    val event = CloudEventBuilder
      .v1()
      .withId("1234-5678-9012-3456")
      .withType("pubsub.message")
      .withSource(java.net.URI.create("https://github.com/cloudevents/spec/pull/123"))
      .withData(json_formatted_data.getBytes())
      .build()

    new StackDriverLogging().accept(event)

    val messages = testLogHandler.getLog.map(_.getMessage())

    assertEquals(
      List("data over the wire: Hello, Feral"),
      messages
    )

  }

  test("No Message sent over the wire should output \'hello World\' default") {

    val pubsub_message = PubsubMessage
      .newBuilder()
      .setData(ByteString.copyFromUtf8(""))
      .putAttributes("attr1", "attr1-value")
      .setMessageId("message-id")
      .setPublishTime(Timestamps.parse("2021-02-05T04:06:14.109Z"))
      .build()

    val message_published_data =
      MessagePublishedData.newBuilder().setMessage(pubsub_message).build()

    val json_formatted_data =
      JsonFormat.printer().alwaysPrintFieldsWithNoPresence().print(message_published_data)

    val event = CloudEventBuilder
      .v1()
      .withId("1234-5678-9012-3456")
      .withType("pubsub.message")
      .withSource(java.net.URI.create("https://github.com/cloudevents/spec/pull/123"))
      .withData(json_formatted_data.getBytes())
      .build()

    new StackDriverLogging().accept(event)

    val messages = testLogHandler.getLog.map(_.getMessage())

    assertEquals(
      List("Hello World"),
      messages
    )
  }
}
