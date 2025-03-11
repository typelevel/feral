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
import feral.lambda.events.DynamoDbStreamEvent
import feral.lambda.events.S3BatchEvent
import feral.lambda.events.SqsEvent
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.trace.SpanKind

trait EventAttributeSource[E] {
  def contextCarrier(e: E): Map[String, String]
  def spanKind: SpanKind
  def attributes(e: E): Attributes
}

object EventAttributeSource {
  def empty[E](sk: SpanKind): EventAttributeSource[E] =
    new EventAttributeSource[E] {
      def contextCarrier(e: E): Map[String, String] =
        Map.empty
      def spanKind: SpanKind = sk
      def attributes(e: E): Attributes =
        Attributes.empty
    }

  implicit def sqsEvent: EventAttributeSource[SqsEvent] =
    new EventAttributeSource[SqsEvent] {
      def contextCarrier(e: SqsEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: SqsEvent): Attributes =
        SqsEventAttributes()
    }

  implicit def dynamoDbStreamEvent: EventAttributeSource[DynamoDbStreamEvent] =
    new EventAttributeSource[DynamoDbStreamEvent] {
      def contextCarrier(e: DynamoDbStreamEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: DynamoDbStreamEvent): Attributes =
        DynamoDbStreamEventAttributes()
    }

  implicit def apiGatewayProxyEvent: EventAttributeSource[ApiGatewayProxyEvent] =
    new EventAttributeSource[ApiGatewayProxyEvent] {
      def contextCarrier(e: ApiGatewayProxyEvent): Map[String, String] =
        e.headers.getOrElse(Map.empty).map { case (k, v) => (k.toString, v) }

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: ApiGatewayProxyEvent): Attributes =
        ApiGatewayProxyEventAttributes()
    }

  implicit def apiGatewayProxyEventV2: EventAttributeSource[ApiGatewayProxyEventV2] =
    new EventAttributeSource[ApiGatewayProxyEventV2] {
      def contextCarrier(e: ApiGatewayProxyEventV2): Map[String, String] =
        e.headers.map { case (k, v) => (k.toString, v) }

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: ApiGatewayProxyEventV2): Attributes =
        ApiGatewayProxyEventAttributes()
    }

  implicit def s3BatchEvent: EventAttributeSource[S3BatchEvent] =
    new EventAttributeSource[S3BatchEvent] {
      def contextCarrier(e: S3BatchEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: S3BatchEvent): Attributes =
        S3BatchEventAttributes(e)
    }
}
