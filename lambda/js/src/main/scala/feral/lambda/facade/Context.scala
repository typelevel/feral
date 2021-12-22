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
import scala.scalajs.js._

@js.native
private[lambda] sealed trait Context extends js.Object {
  def callbackWaitsForEmptyEventLoop: Boolean = js.native
  def functionName: String = js.native
  def functionVersion: String = js.native
  def invokedFunctionArn: String = js.native
  // Note: This should always be a string, but the Netlify CLI currently passes a number.
  // ref: https://github.com/netlify/functions/pull/251
  def memoryLimitInMB: String | Int = js.native
  def awsRequestId: String = js.native
  def logGroupName: String = js.native
  def logStreamName: String = js.native
  def identity: js.UndefOr[CognitoIdentity] = js.native
  def clientContext: js.UndefOr[ClientContext] = js.native
  def getRemainingTimeInMillis(): Double = js.native
}

@js.native
private[lambda] sealed trait CognitoIdentity extends js.Object {
  def cognitoIdentityId: String = js.native
  def cognitoIdentityPoolId: String = js.native
}

@js.native
private[lambda] sealed trait ClientContext extends js.Object {
  def client: ClientContextClient = js.native
  def Custom: js.Any = js.native
  def env: ClientContextEnv = js.native
}

@js.native
private[lambda] sealed trait ClientContextClient extends js.Object {
  def installationId: String = js.native
  def appTitle: String = js.native
  def appVersionName: String = js.native
  def appVersionCode: String = js.native
  def appPackageName: String = js.native
}

@js.native
private[lambda] sealed trait ClientContextEnv extends js.Object {
  def platformVersion: String = js.native
  def platform: String = js.native
  def make: String = js.native
  def model: String = js.native
  def locale: String = js.native
}
