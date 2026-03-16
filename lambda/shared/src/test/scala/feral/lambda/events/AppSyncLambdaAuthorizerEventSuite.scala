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

class AppSyncLambdaAuthorizerEventSuite extends FunSuite {

  test("decoder") {
    import AppSyncLambdaAuthorizerEventSuite._

    val parsed_event = event.as[AppSyncLambdaAuthorizerEvent].toTry.get

    assertEquals(parsed_event.authorizationToken, "ExampleAUTHtoken123123123")
    assertEquals(parsed_event.requestContext.accountId, "111122223333")
    assertEquals(parsed_event.requestContext.apiId, "aaaaaa123123123example123")
    assertEquals(parsed_event.requestContext.channel, "/news/latest")
    assertEquals(parsed_event.requestHeaders("header"), "value")
  }

}

object AppSyncLambdaAuthorizerEventSuite {
    def event = json"""
    {
        "authorizationToken": "ExampleAUTHtoken123123123",
        "requestContext": {
            "apiId": "aaaaaa123123123example123",
            "accountId": "111122223333",
            "requestId": "f4081827-1111-4444-5555-5cf4695f339f",
            "operation": "EVENT_PUBLISH",
            "channelNamespaceName": "news",
            "channel": "/news/latest"
        },
        "requestHeaders": {
            "header": "value"
        }
    }
    """
}
