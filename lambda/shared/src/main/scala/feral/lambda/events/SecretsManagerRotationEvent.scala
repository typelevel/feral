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

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/secretsmanager.d.ts

sealed abstract class SecretsManagerRotationEvent {
  def step: SecretsManagerRotationEvent.Step
  def secretId: String
  def clientRequestToken: String
}

object SecretsManagerRotationEvent {

  def apply(
      step: Step,
      secretId: String,
      clientRequestToken: String
  ): SecretsManagerRotationEvent =
    new Impl(step, secretId, clientRequestToken)

  implicit val decoder: Decoder[SecretsManagerRotationEvent] =
    Decoder.forProduct3(
      "Step",
      "SecretId",
      "ClientRequestToken"
    )(SecretsManagerRotationEvent.apply)

  private final case class Impl(
      step: Step,
      secretId: String,
      clientRequestToken: String
  ) extends SecretsManagerRotationEvent {
    override def productPrefix = "SecretsManagerRotationEvent"
  }

  sealed abstract class Step
  object Step {
    case object CreateSecret extends Step
    case object SetSecret extends Step
    case object TestSecret extends Step
    case object FinishSecret extends Step

    implicit val decoder: Decoder[Step] = Decoder.decodeString.emap {
      case "createSecret" => Right(CreateSecret)
      case "setSecret" => Right(SetSecret)
      case "testSecret" => Right(TestSecret)
      case "finishSecret" => Right(FinishSecret)
      case other => Left(s"Unknown rotation step: $other")
    }
  }
}
