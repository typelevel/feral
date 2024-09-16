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

import munit.FunSuite
import org.typelevel.otel4s.semconv.experimental.attributes.MessagingExperimentalAttributes

/**
 * Check attributes match experimental ones
 */
class LambdaMessageAttributesTest extends FunSuite {

  import LambdaMessageAttributes._
  val otel4s = MessagingExperimentalAttributes

  test("MessagingSystem") {
    val value = "value"
    val ours = MessagingSystem(value)
    val otel = otel4s.MessagingSystem(value)
    assertEquals(ours, otel)
  }

  test("MessagingSystemValue.AwsSqs") {
    val ours = MessagingSystemValue.AwsSqs.value
    val otel = otel4s.MessagingSystemValue.AwsSqs.value
    assertEquals(ours, otel)
  }

  test("MessagingOperationType") {
    val value = "value"
    val ours = MessagingOperationType(value)
    val otel = otel4s.MessagingOperationType(value)
    assertEquals(ours, otel)
  }

  test("MessagingOperationValue.Receive") {
    val ours = MessagingOperationTypeValue.Receive.value
    val otel = otel4s.MessagingOperationTypeValue.Receive.value
    assertEquals(ours, otel)
  }

  test("MessagingMessageId") {
    val value = "value"
    val ours = MessagingMessageId(value)
    val otel = otel4s.MessagingMessageId(value)
    assertEquals(ours, otel)
  }

  test("MessagingDestinationName") {
    val value = "value"
    val ours = MessagingDestinationName(value)
    val otel = otel4s.MessagingDestinationName(value)
    assertEquals(ours, otel)
  }
}
