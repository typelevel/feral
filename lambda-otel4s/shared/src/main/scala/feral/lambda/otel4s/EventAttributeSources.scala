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

trait EventAttributeSources {
  implicit def sqsEvent: EventSpanAttributes[SqsEvent] =
    new EventSpanAttributes[SqsEvent] {
      def contextCarrier(e: SqsEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: SqsEvent): Attributes =
        SqsEventAttributes()
    }

  implicit def dynamoDbStreamEvent: EventSpanAttributes[DynamoDbStreamEvent] =
    new EventSpanAttributes[DynamoDbStreamEvent] {
      def contextCarrier(e: DynamoDbStreamEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: DynamoDbStreamEvent): Attributes =
        DynamoDbStreamEventAttributes()
    }

  implicit def apiGatewayProxyEvent: EventSpanAttributes[ApiGatewayProxyEvent] =
    new EventSpanAttributes[ApiGatewayProxyEvent] {
      def contextCarrier(e: ApiGatewayProxyEvent): Map[String, String] =
        e.headers.getOrElse(Map.empty).map { case (k, v) => (k.toString, v) }

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: ApiGatewayProxyEvent): Attributes =
        ApiGatewayProxyEventAttributes()
    }

  implicit def apiGatewayProxyEventV2: EventSpanAttributes[ApiGatewayProxyEventV2] =
    new EventSpanAttributes[ApiGatewayProxyEventV2] {
      def contextCarrier(e: ApiGatewayProxyEventV2): Map[String, String] =
        e.headers.map { case (k, v) => (k.toString, v) }

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: ApiGatewayProxyEventV2): Attributes =
        ApiGatewayProxyEventAttributes()
    }

  implicit def s3BatchEvent: EventSpanAttributes[S3BatchEvent] =
    new EventSpanAttributes[S3BatchEvent] {
      def contextCarrier(e: S3BatchEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Server

      def attributes(e: S3BatchEvent): Attributes =
        S3BatchEventAttributes(e)
    }

}
