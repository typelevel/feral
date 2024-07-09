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
import cats.effect.Sync
import cats.effect.std.Env
import cats.syntax.functor._
import io.circe.JsonObject
import io.circe.scalajs._

import scala.concurrent.duration._

private[lambda] object ContextPlatform {

  private[lambda] def fromJS[F[_]: Sync: Env](context: facade.Context): F[Context[F]] = Env[F]
    .get("_X_AMZN_TRACE_ID")
    .map(traceId =>
      Context(
        context.functionName,
        context.functionVersion,
        context.invokedFunctionArn,
        context.memoryLimitInMB.toInt,
        context.awsRequestId,
        context.logGroupName,
        context.logStreamName,
        context.identity.toOption.map { identity =>
          CognitoIdentity(identity.cognitoIdentityId, identity.cognitoIdentityPoolId)
        },
        context
          .clientContext
          .toOption
          .map { clientContext =>
            ClientContext(
              ClientContextClient(
                clientContext.client.installationId,
                clientContext.client.appTitle,
                clientContext.client.appVersionName,
                clientContext.client.appVersionCode,
                clientContext.client.appPackageName
              ),
              ClientContextEnv(
                clientContext.env.platformVersion,
                clientContext.env.platform,
                clientContext.env.make,
                clientContext.env.model,
                clientContext.env.locale
              ),
              clientContext
                .custom
                .toOption
                .flatMap(decodeJs[JsonObject](_).toOption)
                .getOrElse(JsonObject.empty)
            )
          },
        Sync[F].delay(context.getRemainingTimeInMillis().millis),
        traceId
      ))
}
