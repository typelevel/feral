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

class SecretsManagerRotationEventSuite extends FunSuite {

  test("decoder createSecret") {
    assertEquals(
      createSecretEvent.as[SecretsManagerRotationEvent].toTry.get,
      expectedCreateSecret)
  }

  test("decoder setSecret") {
    assertEquals(setSecretEvent.as[SecretsManagerRotationEvent].toTry.get, expectedSetSecret)
  }

  test("decoder testSecret") {
    assertEquals(testSecretEvent.as[SecretsManagerRotationEvent].toTry.get, expectedTestSecret)
  }

  test("decoder finishSecret") {
    assertEquals(
      finishSecretEvent.as[SecretsManagerRotationEvent].toTry.get,
      expectedFinishSecret)
  }

  def createSecretEvent = json"""
  {
    "Step": "createSecret",
    "SecretId": "arn:aws:secretsmanager:us-east-1:123456789012:secret:my-secret",
    "ClientRequestToken": "token-123"
  }
  """

  def expectedCreateSecret =
    SecretsManagerRotationEvent(
      step = SecretsManagerRotationEventStep.CreateSecret,
      secretId = "arn:aws:secretsmanager:us-east-1:123456789012:secret:my-secret",
      clientRequestToken = "token-123"
    )

  def setSecretEvent = json"""
  {
    "Step": "setSecret",
    "SecretId": "arn:aws:secretsmanager:us-east-1:123456789012:secret:my-secret",
    "ClientRequestToken": "token-456"
  }
  """

  def expectedSetSecret =
    SecretsManagerRotationEvent(
      step = SecretsManagerRotationEventStep.SetSecret,
      secretId = "arn:aws:secretsmanager:us-east-1:123456789012:secret:my-secret",
      clientRequestToken = "token-456"
    )

  def testSecretEvent = json"""
  {
    "Step": "testSecret",
    "SecretId": "my-secret-id",
    "ClientRequestToken": "token-789"
  }
  """

  def expectedTestSecret =
    SecretsManagerRotationEvent(
      step = SecretsManagerRotationEventStep.TestSecret,
      secretId = "my-secret-id",
      clientRequestToken = "token-789"
    )

  def finishSecretEvent = json"""
  {
    "Step": "finishSecret",
    "SecretId": "my-secret-id",
    "ClientRequestToken": "token-finish"
  }
  """

  def expectedFinishSecret =
    SecretsManagerRotationEvent(
      step = SecretsManagerRotationEventStep.FinishSecret,
      secretId = "my-secret-id",
      clientRequestToken = "token-finish"
    )
}
