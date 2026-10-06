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

package feral.lambda
package events

import io.circe.Decoder
import io.circe.Json
import org.typelevel.ci.CIString

import codecs._

sealed abstract class AppSyncRequestContext {
  def apiId: String
  def accountId: String
  def requestId: String
  def queryString: String
  def operationName: String
  def variables: Map[String, Json]
}

object AppSyncRequestContext {

  def apply(
      apiId: String,
      accountId: String,
      requestId: String,
      queryString: String,
      operationName: String,
      variables: Map[String, Json]
  ): AppSyncRequestContext =
    new Impl(
      apiId,
      accountId,
      requestId,
      queryString,
      operationName,
      variables
    )

  implicit def decoder: Decoder[AppSyncRequestContext] = Decoder.forProduct6(
    "apiId",
    "accountId",
    "requestId",
    "queryString",
    "operationName",
    "variables"
  )(AppSyncRequestContext.apply)

  private final case class Impl(
      apiId: String,
      accountId: String,
      requestId: String,
      queryString: String,
      operationName: String,
      variables: Map[String, Json]
  ) extends AppSyncRequestContext {
    override def productPrefix = "AppSyncRequestContext"
  }
}

sealed abstract class AppSyncLambdaAuthorizerEvent {
  def authorizationToken: String
  def requestContext: AppSyncRequestContext
  def requestHeaders: Map[CIString, String]
}

object AppSyncLambdaAuthorizerEvent {
  def apply(
      authorizationToken: String,
      requestContext: AppSyncRequestContext,
      requestHeaders: Map[CIString, String]
  ): AppSyncLambdaAuthorizerEvent =
    new Impl(
      authorizationToken: String,
      requestContext: AppSyncRequestContext,
      requestHeaders: Map[CIString, String]
    )

  implicit def decoder: Decoder[AppSyncLambdaAuthorizerEvent] = Decoder.forProduct3(
    "authorizationToken",
    "requestContext",
    "requestHeaders"
  )(AppSyncLambdaAuthorizerEvent.apply)

  private final case class Impl(
      authorizationToken: String,
      requestContext: AppSyncRequestContext,
      requestHeaders: Map[CIString, String]
  ) extends AppSyncLambdaAuthorizerEvent {
    override def productPrefix = "AppSyncLambdaAuthorizerEvent"
  }
}
