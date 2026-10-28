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
import io.circe.Json
import io.circe.JsonObject
import io.circe.syntax._

import java.util.Collections
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.chaining._

private[lambda] trait ContextCompanionPlatform {

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
        val env =
          Option(clientContext.getEnvironment())
            .map(_.asScala)
            .getOrElse(Map.empty[String, String])
            .pipe { env =>
              ClientContextEnv(
                env.get("platformVersion").orNull,
                env.get("platform").orNull,
                env.get("make").orNull,
                env.get("model").orNull,
                env.get("locale").orNull
              )
            }

        val maybeClient =
          for {
            client <- Option(clientContext.getClient())
          } yield ClientContextClient(
            client.getInstallationId(),
            client.getAppTitle(),
            client.getAppVersionName(),
            client.getAppVersionCode(),
            client.getAppPackageName()
          )

        val custom =
          JsonObject.fromIterable {
            Option(clientContext.getCustom())
              .getOrElse(Collections.emptyMap[String, String]())
              .asScala
              .view
              .mapValues {
                case null => Json.Null
                case other => other.asJson
              }
          }

        ClientContext(maybeClient, env, custom)
      },
      Sync[F].delay(context.getRemainingTimeInMillis().millis)
    )

}
