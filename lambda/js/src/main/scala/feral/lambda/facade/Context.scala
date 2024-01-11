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

package feral.lambda.facade

import scala.scalajs.js

private[lambda] trait Context extends js.Object {
  def functionName: String
  def functionVersion: String
  def invokedFunctionArn: String
  def memoryLimitInMB: String
  def awsRequestId: String
  def logGroupName: String
  def logStreamName: String
  def identity: js.UndefOr[CognitoIdentity]
  def clientContext: js.UndefOr[ClientContext]
  def getRemainingTimeInMillis(): Double
}

private[lambda] trait CognitoIdentity extends js.Object {
  def cognitoIdentityId: String
  def cognitoIdentityPoolId: String
}

private[lambda] trait ClientContext extends js.Object {
  def client: ClientContextClient
  def custom: js.UndefOr[js.Any]
  def env: ClientContextEnv
}

private[lambda] trait ClientContextClient extends js.Object {
  def installationId: String
  def appTitle: String
  def appVersionName: String
  def appVersionCode: String
  def appPackageName: String
}

private[lambda] trait ClientContextEnv extends js.Object {
  def platformVersion: String
  def platform: String
  def make: String
  def model: String
  def locale: String
}
