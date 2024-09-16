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

import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.trace.SpanKind

// TODO better name
private[otel4s] trait EventSpanAttributes[E] {
  def contextCarrier(e: E): Map[String, String]
  def spanKind: SpanKind
  def attributes(e: E): Attributes
}

private[otel4s] object EventSpanAttributes {
  def empty[E](sk: SpanKind): EventSpanAttributes[E] =
    new EventSpanAttributes[E] {
      def contextCarrier(e: E): Map[String, String] =
        Map.empty
      def spanKind: SpanKind = sk
      def attributes(e: E): Attributes =
        Attributes.empty
    }
}
