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

import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.DynamoDbRecord
import feral.lambda.events.DynamoDbStreamEvent
import feral.lambda.events.S3BatchEvent
import feral.lambda.events.SqsRecord
import org.typelevel.otel4s.Attribute

import LambdaMessageAttributes._
import LambdaContextAttributes._

object SqsEventAttributes {
  def apply(): List[Attribute[_]] =
    List(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem("aws_sqs")
    )
}

object SqsRecordAttributes {
  def apply(e: SqsRecord): List[Attribute[_]] =
    List(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingOperation(MessagingOperationValue.Receive.value),
      MessagingMessageId(e.messageId)
    )
}

object DynamoDbStreamEventAttributes {
  def apply(): List[Attribute[_]] =
    List(
      FaasTrigger(FaasTriggerValue.Datasource.value),
      MessagingSystem("aws_sqs")
    )
}

object DynamoDbRecordAttributes {
  def apply(e: DynamoDbRecord): List[Attribute[_]] =
    List(
      e.eventId.map(MessagingMessageId(_)),
      Some(FaasTrigger(FaasTriggerValue.Datasource.value)),
      Some(MessagingOperation(MessagingOperationValue.Receive.value))
    ).flatten
}

object ApiGatewayProxyEventAttributes {
  def apply(): List[Attribute[_]] =
    List(
      FaasTrigger(FaasTriggerValue.Http.value),
      MessagingOperation(MessagingOperationValue.Receive.value)
    )
}

object S3BatchEventAttributes {
  def apply(e: S3BatchEvent): List[Attribute[_]] =
    List(
      MessagingMessageId(e.invocationId),
      FaasTrigger(FaasTriggerValue.Datasource.value),
      MessagingOperation(MessagingOperationValue.Receive.value)
    )
}
