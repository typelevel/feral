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

import org.typelevel.otel4s.AttributeKey

/**
 * Temporary aliases for Lambda message-specific attributes in otel4s-semconv-experimental
 */
private[otel4s] object LambdaMessageAttributes {
  val MessagingSystem = AttributeKey.string("messaging.system")
  object MessagingSystemValue {
    object AwsSqs {
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
