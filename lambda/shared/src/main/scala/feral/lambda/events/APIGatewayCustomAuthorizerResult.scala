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
package events

import io.circe.Decoder

import java.time.LocalDate

import codecs.decodeDate

sealed abstract class APIGatewayCustomAuthorizerEventResult {
  def principalId: String
  def policyDocument: PolicyDocument
}

object APIGatewayCustomAuthorizerEventResult {

  def apply(
      principalId: String,
      policyDocument: PolicyDocument): APIGatewayCustomAuthorizerEventResult =
    Impl(principalId, policyDocument)

  implicit def decoder: Decoder[APIGatewayCustomAuthorizerEventResult] = Decoder.forProduct2(
    "principalId",
    "policyDocument"
  )(Impl.apply)

  private case class Impl(
      principalId: String,
      policyDocument: PolicyDocument
  ) extends APIGatewayCustomAuthorizerEventResult {
    override def productPrefix = "APIGatewayCustomAuthorizerEventResult"
  }
}

sealed abstract class PolicyDocument {
  def version: LocalDate
  def statement: List[Statement]
}

object PolicyDocument {
  def apply(version: LocalDate, statement: List[Statement]): PolicyDocument =
    Impl(version, statement)

  implicit def decoder: Decoder[PolicyDocument] = Decoder.forProduct2(
    "Version",
    "Statement"
  )(Impl.apply)

  private case class Impl(
      version: LocalDate,
      statement: List[Statement]
  ) extends PolicyDocument {
    override def productPrefix = "PolicyDocument"
  }
}

sealed abstract class Statement {
  def action: String
  def effect: String
  def resource: String
}

object Statement {
  def apply(action: String, effect: String, resource: String): Statement =
    Impl(action, effect, resource)

  implicit def decoder: Decoder[Statement] = Decoder.forProduct3(
    "Action",
    "Effect",
    "Resource"
  )(Impl.apply)

  private case class Impl(
      action: String,
      effect: String,
      resource: String
  ) extends Statement {
    override def productPrefix = "Statement"
  }
}
