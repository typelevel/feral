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

import io.circe.Decoder

// AWS Config custom rule Lambda event. DefinitelyTyped trigger/config is TODO.
// See: https://docs.aws.amazon.com/config/latest/developerguide/evaluate-config_develop-rules_lambda-functions.html
// invokingEvent and ruleParameters are JSON strings; parse separately if needed.

sealed abstract class ConfigEvent {
  def invokingEvent: String
  def ruleParameters: String
  def resultToken: String
  def eventLeftScope: Boolean
  def executionRoleArn: String
  def configRuleArn: String
  def configRuleName: String
  def configRuleId: String
  def accountId: String
  def version: String
}

object ConfigEvent {
  def apply(
      invokingEvent: String,
      ruleParameters: String,
      resultToken: String,
      eventLeftScope: Boolean,
      executionRoleArn: String,
      configRuleArn: String,
      configRuleName: String,
      configRuleId: String,
      accountId: String,
      version: String
  ): ConfigEvent =
    new Impl(
      invokingEvent,
      ruleParameters,
      resultToken,
      eventLeftScope,
      executionRoleArn,
      configRuleArn,
      configRuleName,
      configRuleId,
      accountId,
      version
    )

  implicit val decoder: Decoder[ConfigEvent] =
    Decoder.forProduct10(
      "invokingEvent",
      "ruleParameters",
      "resultToken",
      "eventLeftScope",
      "executionRoleArn",
      "configRuleArn",
      "configRuleName",
      "configRuleId",
      "accountId",
      "version"
    )(ConfigEvent.apply)

  private final case class Impl(
      invokingEvent: String,
      ruleParameters: String,
      resultToken: String,
      eventLeftScope: Boolean,
      executionRoleArn: String,
      configRuleArn: String,
      configRuleName: String,
      configRuleId: String,
      accountId: String,
      version: String
  ) extends ConfigEvent {
    override def productPrefix = "ConfigEvent"
  }
}
