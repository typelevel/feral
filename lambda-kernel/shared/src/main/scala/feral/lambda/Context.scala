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

import cats.Applicative
import cats.~>
import io.circe.JsonObject

import scala.concurrent.duration.FiniteDuration

sealed abstract class Context[F[_]] {
  def functionName: String
  def functionVersion: String
  def invokedFunctionArn: String
  def memoryLimitInMB: Int
  def awsRequestId: String
  def xRayTraceId: Option[String]
  def logGroupName: String
  def logStreamName: String
  def identity: Option[CognitoIdentity]
  def clientContext: Option[ClientContext]
  def remainingTime: F[FiniteDuration]

  final def mapK[G[_]](f: F ~> G): Context[G] =
    new Context.Impl(
      functionName,
      functionVersion,
      invokedFunctionArn,
      memoryLimitInMB,
      awsRequestId,
      xRayTraceId,
      logGroupName,
      logStreamName,
      identity,
      clientContext,
      f(remainingTime)
    )

}

object Context {
  def apply[F[_]](
      functionName: String,
      functionVersion: String,
      invokedFunctionArn: String,
      memoryLimitInMB: Int,
      awsRequestId: String,
      xRayTraceId: Option[String],
      logGroupName: String,
      logStreamName: String,
      identity: Option[CognitoIdentity],
      clientContext: Option[ClientContext],
      remainingTime: F[FiniteDuration]
  )(implicit F: Applicative[F]): Context[F] = {
    val _ = F // might be useful for future compatibility
    new Impl(
      functionName,
      functionVersion,
      invokedFunctionArn,
      memoryLimitInMB,
      awsRequestId,
      xRayTraceId,
      logGroupName,
      logStreamName,
      identity,
      clientContext,
      remainingTime
    )
  }

  private final case class Impl[F[_]](
      functionName: String,
      functionVersion: String,
      invokedFunctionArn: String,
      memoryLimitInMB: Int,
      awsRequestId: String,
      xRayTraceId: Option[String],
      logGroupName: String,
      logStreamName: String,
      identity: Option[CognitoIdentity],
      clientContext: Option[ClientContext],
      remainingTime: F[FiniteDuration]
  ) extends Context[F] {
    override def productPrefix = "Context"
  }
}

sealed abstract class CognitoIdentity {
  def identityId: String
  def identityPoolId: String
}

object CognitoIdentity {
  def apply(identityId: String, identityPoolId: String): CognitoIdentity =
    new Impl(identityId, identityPoolId)

  private final case class Impl(
      val identityId: String,
      val identityPoolId: String
  ) extends CognitoIdentity {
    override def productPrefix = "CognitoIdentity"
  }
}

sealed abstract class ClientContext {
  def client: ClientContextClient
  def env: ClientContextEnv
  def custom: JsonObject
}

object ClientContext {
  def apply(
      client: ClientContextClient,
      env: ClientContextEnv,
      custom: JsonObject
  ): ClientContext =
    new Impl(client, env, custom)

  private final case class Impl(
      client: ClientContextClient,
      env: ClientContextEnv,
      custom: JsonObject
  ) extends ClientContext {
    override def productPrefix = "ClientContext"
  }
}

sealed abstract class ClientContextClient {
  def installationId: String
  def appTitle: String
  def appVersionName: String
  def appVersionCode: String
  def appPackageName: String
}

object ClientContextClient {
  def apply(
      installationId: String,
      appTitle: String,
      appVersionName: String,
      appVersionCode: String,
      appPackageName: String
  ): ClientContextClient =
    new Impl(installationId, appTitle, appVersionName, appVersionCode, appPackageName)

  private final case class Impl(
      installationId: String,
      appTitle: String,
      appVersionName: String,
      appVersionCode: String,
      appPackageName: String
  ) extends ClientContextClient {
    override def productPrefix = "ClientContextClient"
  }
}

sealed abstract class ClientContextEnv {
  def platformVersion: String
  def platform: String
  def make: String
  def model: String
  def locale: String
}

object ClientContextEnv {
  def apply(
      platformVersion: String,
      platform: String,
      make: String,
      model: String,
      locale: String): ClientContextEnv =
    new Impl(platformVersion, platform, make, model, locale)

  private final case class Impl(
      platformVersion: String,
      platform: String,
      make: String,
      model: String,
      locale: String
  ) extends ClientContextEnv {
    override def productPrefix = "ClientContextEnv"
  }
}
