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

import cats.~>

import scala.concurrent.duration.FiniteDuration

final class Context[F[_]] private[lambda] (
    val functionName: String,
    val functionVersion: String,
    val invokedFunctionArn: String,
    val memoryLimitInMb: Int,
    val awsRequestId: String,
    val logGroupName: String,
    val logStreamName: String,
    val identity: Option[CognitoIdentity],
    val clientContext: Option[ClientContext],
    val remainingTime: F[FiniteDuration]
) {
  def mapK[G[_]](f: F ~> G): Context[G] = new Context(
    functionName,
    functionVersion,
    invokedFunctionArn,
    memoryLimitInMb,
    awsRequestId,
    logGroupName,
    logStreamName,
    identity,
    clientContext,
    f(remainingTime))
}

object Context extends ContextCompanionPlatform

final class CognitoIdentity(
    val identityId: String,
    val identityPoolId: String
)

final class ClientContext(
    val client: ClientContextClient,
    val env: ClientContextEnv
)

final class ClientContextClient(
    val installationId: String,
    val appTitle: String,
    val appVersionName: String,
    val appVersionCode: String,
    val appPackageName: String
)

final class ClientContextEnv(
    val platformVersion: String,
    val platform: String,
    val make: String,
    val model: String,
    val locale: String
)
