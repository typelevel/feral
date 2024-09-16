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

import feral.lambda.events.DynamoDbRecord
import feral.lambda.events.S3BatchEvent
import feral.lambda.events.SqsRecord
import org.typelevel.otel4s.Attributes

import LambdaMessageAttributes._
import LambdaContextAttributes._

object SqsEventAttributes {
  def apply(): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem(MessagingSystemValue.AwsSqs.value)
    )
}

object SqsRecordAttributes {
  def apply(e: SqsRecord): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingOperationType(MessagingOperationTypeValue.Receive.value),
      MessagingMessageId(e.messageId)
    )
}

object DynamoDbStreamEventAttributes {
  def apply(): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Datasource.value),
      MessagingSystem(MessagingSystemValue.AwsSqs.value)
    )
}

object DynamoDbRecordAttributes {
  def apply(e: DynamoDbRecord): Attributes = {
    Attributes(
      FaasTrigger(FaasTriggerValue.Datasource.value),
      MessagingOperationType(MessagingOperationTypeValue.Receive.value)
    ) ++ e.eventId.map(MessagingMessageId(_))
  }
}

object ApiGatewayProxyEventAttributes {
  def apply(): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Http.value),
      MessagingOperationType(MessagingOperationTypeValue.Receive.value)
    )
}

object S3BatchEventAttributes {
  def apply(e: S3BatchEvent): Attributes =
    Attributes(
      MessagingMessageId(e.invocationId),
      FaasTrigger(FaasTriggerValue.Datasource.value),
      MessagingOperationType(MessagingOperationTypeValue.Receive.value)
    )
}
