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
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.DynamoDbRecord
import feral.lambda.events.SqsEvent
import feral.lambda.events.SqsRecord
import org.typelevel.otel4s.Attributes

import OtelAttributes._

object SqsEventAttributes {
  def apply(e: SqsEvent): Attributes = {
    val base = Attributes(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem(MessagingSystemValue.AwsSqs.value),
      MessagingOperationType(MessagingOperationTypeValue.Process.value)
    )

    SqsAttributesHelpers
      .singleSqsQueueName(e)
      .fold(base)(name => base ++ Attributes(MessagingDestinationName(name)))
  }
}

object SqsRecordAttributes {
  def apply(e: SqsRecord): Attributes = {
    val base = Attributes(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem(MessagingSystemValue.AwsSqs.value),
      MessagingOperationType(MessagingOperationTypeValue.Process.value),
      MessagingMessageId(e.messageId)
    )

    SqsAttributesHelpers
      .sqsQueueNameFromArn(e.eventSourceArn)
      .fold(base)(name => base ++ Attributes(MessagingDestinationName(name)))
  }
}

object DynamoDbStreamEventAttributes {
  def apply(): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Datasource.value)
    )
}

object DynamoDbRecordAttributes {
  def apply(unused: DynamoDbRecord): Attributes = {
    val _ = unused
    Attributes(
      FaasTrigger(FaasTriggerValue.Datasource.value)
    )
  }
}

object ApiGatewayProxyEventAttributes {
  def apply(e: ApiGatewayProxyEvent): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Http.value),
      HttpRoute(e.resource)
    )
}

object ApiGatewayProxyEventV2Attributes {
  def apply(e: ApiGatewayProxyEventV2): Attributes =
    Attributes(
      FaasTrigger(FaasTriggerValue.Http.value),
      HttpRoute(e.rawPath)
    )
}

object S3BatchEventAttributes {
  def apply(): Attributes = {
    Attributes(
      FaasTrigger(FaasTriggerValue.Other.value)
    )
  }
}

private[otel4s] object SqsAttributesHelpers {
  def sqsQueueNameFromArn(arn: String): Option[String] =
    arn.split(":", 6).lift(5).filter(_.nonEmpty)

  def singleSqsQueueName(e: SqsEvent): Option[String] = {
    val queueNames = e.records.flatMap(r => sqsQueueNameFromArn(r.eventSourceArn)).distinct
    if (queueNames.size == 1) queueNames.headOption else None
  }

  def sqsEventSpanName(e: SqsEvent): Option[String] = {
    val names = e
      .records
      .map(r => sqsQueueNameFromArn(r.eventSourceArn).getOrElse(r.eventSource))
      .distinct

    if (names.isEmpty) None
    else if (names.size == 1) Some(s"${names.head} process")
    else Some("multiple_sources process")
  }
}
