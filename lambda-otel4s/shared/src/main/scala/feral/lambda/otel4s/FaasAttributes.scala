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
import org.typelevel.otel4s.AttributeKey

/**
 * Temporary aliases for Lambda message-specific attributes in otel4s-semconv-experimental
 */
object LambdaMessageAttributes {
  val MessagingSystem = AttributeKey.string("messaging.system")
  object MessagingSystemValue {
    object Sqs {
      val value = "aws_sqs"
    }
  }
  val MessagingOperation = AttributeKey.string("messaging.operation")
  object MessagingOperationValue {
    object Receive {
      val value = "receive"
    }
  }
  val MessagingMessageId = AttributeKey.string("messaging.message.id")
  val MessagingDestinationName = AttributeKey.string("messaging.destination.name")
}

/**
 * Temporary aliases for Lambda platform attributes in otel4s-semconv-experimental
 */
object LambdaContextAttributes {
  val FaasInvocationId = AttributeKey.string("faas.invocation_id")
  val FaasTrigger = AttributeKey.string("faas.trigger")
  object FaasTriggerValue {
    object Pubsub {
      val value = "pubsub"
    }
  }
  // ARN
  val CloudResourceId = AttributeKey.string("cloud.resource_id")
  val FaasInstance = AttributeKey.string("faas.instance")
  val FaasMaxMemory = AttributeKey.long("faas.max_memory")
  val FaasName = AttributeKey.string("faas.name")
  val FaasVersion = AttributeKey.string("faas.version")
  val CloudProvider = AttributeKey.string("cloud.provider")
  object CloudProviderValue {
    object Aws {
      val value = "aws"
    }
  }

  def apply[F[_]](context: Context[F]): List[Attribute[_]] = {
    List(
      CloudProvider(CloudProviderValue.Aws.value),
      CloudResourceId(context.invokedFunctionArn),
      FaasInstance(context.logStreamName),
      FaasMaxMemory(context.memoryLimitInMB.toLong),
      FaasName(context.functionName),
      FaasVersion(context.functionVersion)
    )
  }

}
