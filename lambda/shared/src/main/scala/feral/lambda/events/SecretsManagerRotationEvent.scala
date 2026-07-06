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

import scala.util.Try

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/secretsmanager.d.ts
// https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-lambda-function-overview.html

sealed abstract class SecretsManagerRotationEventStep

object SecretsManagerRotationEventStep {

  case object CreateSecret extends SecretsManagerRotationEventStep
  case object SetSecret extends SecretsManagerRotationEventStep
  case object TestSecret extends SecretsManagerRotationEventStep
  case object FinishSecret extends SecretsManagerRotationEventStep

  private[events] implicit val decoder: Decoder[SecretsManagerRotationEventStep] =
    Decoder.decodeString.emapTry {
      case "createSecret" => Try(CreateSecret)
      case "setSecret" => Try(SetSecret)
      case "testSecret" => Try(TestSecret)
      case "finishSecret" => Try(FinishSecret)
      case s => scala.util.Failure(new IllegalArgumentException(s"Unknown step: $s"))
    }
}

sealed abstract class SecretsManagerRotationEvent {
  def step: SecretsManagerRotationEventStep
  def secretId: String
  def clientRequestToken: String
}

object SecretsManagerRotationEvent {

  def apply(
      step: SecretsManagerRotationEventStep,
      secretId: String,
      clientRequestToken: String
  ): SecretsManagerRotationEvent =
    new Impl(step, secretId, clientRequestToken)

  private[events] implicit val decoder: Decoder[SecretsManagerRotationEvent] =
    Decoder.instance(c =>
      for {
        step <- c.get[SecretsManagerRotationEventStep]("Step")
        secretId <- c.get[String]("SecretId")
        clientRequestToken <- c.get[String]("ClientRequestToken")
      } yield SecretsManagerRotationEvent(step, secretId, clientRequestToken))

  private final case class Impl(
      step: SecretsManagerRotationEventStep,
      secretId: String,
      clientRequestToken: String
  ) extends SecretsManagerRotationEvent {
    override def productPrefix = "SecretsManagerRotationEvent"
  }
}
