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

class ConfigEventSuite extends FunSuite {
  import ConfigEventSuite._

  test("decoder") {
    assertEquals(configEvent.as[ConfigEvent].toTry.get, decoded)
  }
}

object ConfigEventSuite {

  val decoded: ConfigEvent =
    ConfigEvent(
      invokingEvent = """{"messageType":"ConfigurationItemChangeNotification","configurationItem":{}}""",
      ruleParameters = """{"desiredInstanceType":"t2.micro"}""",
      resultToken = "testResultToken123",
      eventLeftScope = false,
      executionRoleArn = "arn:aws:iam::123456789012:role/config-role",
      configRuleArn = "arn:aws:config:us-east-1:123456789012:config-rule/config-rule-012345",
      configRuleName = "instance-type-check",
      configRuleId = "config-rule-012345",
      accountId = "123456789012",
      version = "1.0"
    )

  // https://docs.aws.amazon.com/config/latest/developerguide/evaluate-config_develop-rules_lambda-functions.html
  val configEvent: Json = json"""
    {
      "invokingEvent": "{\"messageType\":\"ConfigurationItemChangeNotification\",\"configurationItem\":{}}",
      "ruleParameters": "{\"desiredInstanceType\":\"t2.micro\"}",
      "resultToken": "testResultToken123",
      "eventLeftScope": false,
      "executionRoleArn": "arn:aws:iam::123456789012:role/config-role",
      "configRuleArn": "arn:aws:config:us-east-1:123456789012:config-rule/config-rule-012345",
      "configRuleName": "instance-type-check",
      "configRuleId": "config-rule-012345",
      "accountId": "123456789012",
      "version": "1.0"
    }
  """
}
