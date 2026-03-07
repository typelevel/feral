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

class SecretsManagerRotationEventSuite extends FunSuite {
  import SecretsManagerRotationEventSuite._

  test("decoder") {
    assertEquals(createSecretEvent.as[SecretsManagerRotationEvent].toTry.get, decodedCreateSecret)
    assertEquals(
      finishSecretEvent.as[SecretsManagerRotationEvent].toTry.get,
      decodedFinishSecret)
  }
}

object SecretsManagerRotationEventSuite {

  val decodedCreateSecret: SecretsManagerRotationEvent =
    SecretsManagerRotationEvent(
      SecretsManagerRotationEvent.Step.CreateSecret,
      "arn:aws:secretsmanager:us-east-1:123456789012:secret:MyTestSecret-a1b2c3",
      "550e8400-e29b-41d4-a716-446655440000"
    )

  val decodedFinishSecret: SecretsManagerRotationEvent =
    SecretsManagerRotationEvent(
      SecretsManagerRotationEvent.Step.FinishSecret,
      "arn:aws:secretsmanager:us-east-1:123456789012:secret:MyTestSecret-a1b2c3",
      "550e8400-e29b-41d4-a716-446655440000"
    )

  // https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-lambda-function-overview.html
  val createSecretEvent: Json = json"""
    {
      "Step": "createSecret",
      "SecretId": "arn:aws:secretsmanager:us-east-1:123456789012:secret:MyTestSecret-a1b2c3",
      "ClientRequestToken": "550e8400-e29b-41d4-a716-446655440000"
    }
  """

  val finishSecretEvent: Json = json"""
    {
      "Step": "finishSecret",
      "SecretId": "arn:aws:secretsmanager:us-east-1:123456789012:secret:MyTestSecret-a1b2c3",
      "ClientRequestToken": "550e8400-e29b-41d4-a716-446655440000"
    }
  """
}
