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
import com.amazonaws.services.lambda.runtime
import io.circe.JsonObject
import io.circe.jawn.parse

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

private[lambda] object ContextPlatform {

  private[lambda] def fromJava[F[_]: Sync](context: runtime.Context): Context[F] =
    Context(
      context.getFunctionName(),
      context.getFunctionVersion(),
      context.getInvokedFunctionArn(),
      context.getMemoryLimitInMB(),
      context.getAwsRequestId(),
      context.getLogGroupName(),
      context.getLogStreamName(),
      Option(context.getIdentity()).map { identity =>
        CognitoIdentity(identity.getIdentityId(), identity.getIdentityPoolId())
      },
      Option(context.getClientContext()).map { clientContext =>
        ClientContext(
          ClientContextClient(
            clientContext.getClient().getInstallationId(),
            clientContext.getClient().getAppTitle(),
            clientContext.getClient().getAppVersionName(),
            clientContext.getClient().getAppVersionCode(),
            clientContext.getClient().getAppPackageName()
          ),
          ClientContextEnv(
            clientContext.getEnvironment().get("platformVersion"),
            clientContext.getEnvironment().get("platform"),
            clientContext.getEnvironment().get("make"),
            clientContext.getEnvironment().get("model"),
            clientContext.getEnvironment().get("locale")
          ),
          JsonObject.fromIterable(clientContext.getCustom().asScala.view.flatMap {
            case (k, v) =>
              parse(v).toOption.map(k -> _)
          })
        )
      },
      Sync[F].delay(context.getRemainingTimeInMillis().millis),
      None
    )

}
