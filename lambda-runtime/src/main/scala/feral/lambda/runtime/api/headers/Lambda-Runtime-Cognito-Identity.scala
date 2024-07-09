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

package feral.lambda.runtime.api.headers

import cats.syntax.all._
import feral.lambda.CognitoIdentity
import io.circe.Decoder
import io.circe._
import io.circe.jawn._
import io.circe.syntax.EncoderOps
import org.http4s._
import org.typelevel.ci._

/**
 * Information about the Amazon Cognito identity provider when invoked through the AWS Mobile
 * SDK.
 */
private[runtime] final class `Lambda-Runtime-Cognito-Identity`(val value: CognitoIdentity)

private[runtime] object `Lambda-Runtime-Cognito-Identity` {

  def apply(value: CognitoIdentity) = new `Lambda-Runtime-Cognito-Identity`(value)

  final val name = ci"Lambda-Runtime-Cognito-Identity"

  private[headers] def parser(s: String): ParseResult[`Lambda-Runtime-Cognito-Identity`] =
    decode[CognitoIdentity](s).bimap(
      e => ParseFailure(name.toString, e.toString),
      `Lambda-Runtime-Cognito-Identity`(_)
    )

  implicit val headerInstance: Header[`Lambda-Runtime-Cognito-Identity`, Header.Single] =
    Header.create(name, _.value.asJson.toString, parser)

  implicit val cognitoIdentityDecoder: Decoder[CognitoIdentity] =
    Decoder.forProduct2("identity_id", "identity_pool_id")(
      (identityId: String, poolId: String) => CognitoIdentity(identityId, poolId))

  implicit val cognitoIdentityEncoder: Encoder[CognitoIdentity] =
    Encoder.forProduct2("identity_id", "identity_pool_id")(c =>
      (c.identityId, c.identityPoolId))
}
