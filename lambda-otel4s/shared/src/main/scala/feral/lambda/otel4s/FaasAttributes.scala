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
import org.typelevel.otel4s.semconv.resource.attributes.ResourceAttributes
import org.typelevel.otel4s.semconv.trace.attributes.SemanticAttributes

object LambdaContextAttributes {
  val InvocationId = SemanticAttributes.FaasInvocationId
  val FaasTrigger = SemanticAttributes.FaasTrigger
  // ARN
  val CloudResourceId = ResourceAttributes.CloudResourceId
  // log stream name
  val FaasInstance = ResourceAttributes.FaasInstance
  val FaasMaxMemory = ResourceAttributes.FaasMaxMemory
  val FaasName = ResourceAttributes.FaasName
  val FaasVersion = ResourceAttributes.FaasVersion
  val CloudProvider = ResourceAttributes.CloudProvider

  def apply[F[_]](context: Context[F]): List[Attribute[_]] = {
    List(
      CloudProvider(ResourceAttributes.CloudProviderValue.Aws.value),
      CloudResourceId(context.invokedFunctionArn),
      FaasInstance(context.logStreamName),
      FaasMaxMemory(context.memoryLimitInMB.toLong),
      FaasName(context.functionName),
      FaasVersion(context.functionVersion)
    )
  }

}
