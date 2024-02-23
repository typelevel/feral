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

import feral.lambda.events.SqsEvent
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.SpanKind

package object implicits {
  implicit def sqsEventSpanAttributes: EventSpanAttributes[SqsEvent] =
    new EventSpanAttributes[SqsEvent] {
      def contextCarrier(e: SqsEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: SqsEvent): List[Attribute[_]] =
        SqsEventTraceAttributes()
    }
}
