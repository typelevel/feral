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

package feral.netlify

import cats.effect.Sync
import cats.~>
import io.circe.Json
import io.circe.scalajs._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

final class Context[F[_]] private[netlify] (
    val functionName: String,
    val functionVersion: String,
    val invokedFunctionArn: String,
    val memoryLimitInMB: Int,
    val awsRequestId: String,
    val logGroupName: String,
    val logStreamName: String,
    val identity: Option[Map[String, Json]],
    val clientContext: Option[Map[String, Json]],
    val remainingTime: F[FiniteDuration]
) {
  def mapK[G[_]](f: F ~> G): Context[G] = new Context(
    functionName,
    functionVersion,
    invokedFunctionArn,
    memoryLimitInMB,
    awsRequestId,
    logGroupName,
    logStreamName,
    identity,
    clientContext,
    f(remainingTime))
}

object Context {
  private[netlify] def fromJS[F[_]: Sync](context: facade.Context): Context[F] =
    new Context(
      context.functionName,
      context.functionVersion,
      context.invokedFunctionArn,
      (context.memoryLimitInMB: Any) match {
        case s: String => s.toInt
        case i: Int => i
      },
      context.awsRequestId,
      context.logGroupName,
      context.logStreamName,
      context.identity.toOption.flatMap(Option(_)).map {
        decodeJs[Map[String, Json]](_).toOption.get
      },
      context.clientContext.toOption.flatMap(Option(_)).map {
        decodeJs[Map[String, Json]](_).toOption.get
      },
      Sync[F].delay(context.getRemainingTimeInMillis().millis)
    )
}
