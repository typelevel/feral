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

package feral.lambda.otel4s

import feral.lambda.Context
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes

object LambdaContextTraceAttributes {
  val InvocationId = FaasExperimentalAttributes.FaasInvocationId
  val FaasTrigger = FaasExperimentalAttributes.FaasTrigger
  // ARN
  val CloudResourceId = CloudExperimentalAttributes.CloudResourceId
  // log stream name
  val FaasInstance = FaasExperimentalAttributes.FaasInstance
  val FaasMaxMemory = FaasExperimentalAttributes.FaasMaxMemory
  val FaasName = FaasExperimentalAttributes.FaasName
  val FaasVersion = FaasExperimentalAttributes.FaasVersion
  val CloudProvider = CloudExperimentalAttributes.CloudProvider

  def apply[F[_]](context: Context[F]): List[Attribute[_]] = {
    List(
      CloudProvider(CloudExperimentalAttributes.CloudProviderValue.Aws.value),
      CloudResourceId(context.invokedFunctionArn),
      FaasInstance(context.logStreamName),
      FaasMaxMemory(context.memoryLimitInMB.toLong),
      FaasName(context.functionName),
      FaasVersion(context.functionVersion)
    )
  }

}
