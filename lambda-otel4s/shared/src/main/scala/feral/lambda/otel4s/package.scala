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

import feral.lambda.events.SqsMessageAttribute
import feral.lambda.events.SqsRecord
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.trace.attributes.SemanticAttributes
import org.typelevel.otel4s.semconv.trace.attributes.SemanticAttributes.FaasTriggerValue
import org.typelevel.otel4s.trace.SpanKind

protected trait EventTraceContext[E] {
  def contextCarrier(e: E): Map[String, String]
  def spanKind: SpanKind
  def attributes(e: E): List[Attribute[_]]
}

package object otel4s {
  implicit def sqsRecordEventTextMap: EventTraceContext[SqsRecord] =
    new EventTraceContext[SqsRecord] {
      def contextCarrier(e: SqsRecord): Map[String, String] =
        e.messageAttributes.collect { case (k, SqsMessageAttribute.String(v)) => k -> v }
      def spanKind: SpanKind = SpanKind.Consumer
      def attributes(e: SqsRecord): List[Attribute[_]] = {
        List(
          SemanticAttributes.FaasTrigger(FaasTriggerValue.Pubsub.value)
        )
      }
    }

}

object EventSemanticAttributes {
  def sqsRecord(e: SqsRecord): List[Attribute[_]] = {
    import SemanticAttributes._

    List(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem("aws_sqs"),
      MessagingOperation(MessagingOperationValue.Receive.value),
      MessagingMessageId(e.messageId),
      MessagingDestinationName(e.eventSource),
    )
  }
}
