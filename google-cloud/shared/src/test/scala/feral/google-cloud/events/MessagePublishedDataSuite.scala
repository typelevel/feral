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

package feral.googlecloud.events

import io.circe.literal._
import munit.FunSuite
import scodec.bits.ByteVector

import java.time.Instant

class MessagePublishedDataSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[MessagePublishedData].toTry.get, result)
  }

  def event = json"""
    {
      "subscription": "projects/my-project/subscriptions/my-subscription",
      "message": {
        "attributes": {
            "attr1":"attr1-value"
        },
        "data": "dGVzdCBtZXNzYWdlIDM=",
        "messageId": "message-id",
        "publishTime":"2021-02-05T04:06:14.109Z"
      }
    }
  """

  def result = MessagePublishedData(
    subscription = "projects/my-project/subscriptions/my-subscription",
    message = PubsubMessage(
      data = ByteVector.fromBase64("dGVzdCBtZXNzYWdlIDM=").get,
      attributes = Map("attr1" -> "attr1-value"),
      messageId = "message-id",
      publishTime = Instant.parse("2021-02-05T04:06:14.109Z"),
      orderingKey = None
    )
  )

}
