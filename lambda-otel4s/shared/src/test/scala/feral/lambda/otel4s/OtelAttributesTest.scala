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
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.MessagingExperimentalAttributes

/**
 * Check attributes match experimental ones
 */
class OtelAttributesTest extends FunSuite {

  import OtelAttributes._
  val otel4s = FaasExperimentalAttributes
  val otel4sCloud = CloudExperimentalAttributes
  val otel4sMessaging = MessagingExperimentalAttributes

  test("FaasInvocationId") {
    val value = "value"
    val ours = FaasInvocationId(value)
    val otel = otel4s.FaasInvocationId(value)
    assertEquals(ours, otel)
  }

  test("FaasTrigger") {
    val value = "value"
    val ours = FaasTrigger(value)
    val otel = otel4s.FaasTrigger(value)
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.PubSub") {
    val ours = FaasTriggerValue.Pubsub.value
    val otel = otel4s.FaasTriggerValue.Pubsub.value
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.Datasource") {
    val ours = FaasTriggerValue.Datasource.value
    val otel = otel4s.FaasTriggerValue.Datasource.value
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.Http") {
    val ours = FaasTriggerValue.Http.value
    val otel = otel4s.FaasTriggerValue.Http.value
    assertEquals(ours, otel)
  }

  test("CloudResourceId") {
    val value = "value"
    val ours = CloudResourceId(value)
    val otel = otel4sCloud.CloudResourceId(value)
    assertEquals(ours, otel)
  }

  test("FaasInstance") {
    val value = "value"
    val ours = FaasInstance(value)
    val otel = otel4s.FaasInstance(value)
    assertEquals(ours, otel)
  }

  test("FaasMaxMemory") {
    val value = 0L
    val ours = FaasMaxMemory(value)
    val otel = otel4s.FaasMaxMemory(value)
    assertEquals(ours, otel)
  }

  test("FaasName") {
    val value = "value"
    val ours = FaasName(value)
    val otel = otel4s.FaasName(value)
    assertEquals(ours, otel)
  }

  test("FaasVersion") {
    val value = "value"
    val ours = FaasVersion(value)
    val otel = otel4s.FaasVersion(value)
    assertEquals(ours, otel)
  }

  test("CloudProvider") {
    val value = "value"
    val ours = CloudProvider(value)
    val otel = otel4sCloud.CloudProvider(value)
    assertEquals(ours, otel)
  }

  test("CloudProviderValue.Aws") {
    val ours = CloudProviderValue.Aws.value
    val otel = otel4sCloud.CloudProviderValue.Aws.value
    assertEquals(ours, otel)
  }

  test("MessagingSystem") {
    val value = "value"
    val ours = MessagingSystem(value)
    val otel = otel4sMessaging.MessagingSystem(value)
    assertEquals(ours, otel)
  }

  test("MessagingSystemValue.AwsSqs") {
    val ours = MessagingSystemValue.AwsSqs.value
    val otel = otel4sMessaging.MessagingSystemValue.AwsSqs.value
    assertEquals(ours, otel)
  }

  test("MessagingOperationType") {
    val value = "value"
    val ours = MessagingOperationType(value)
    val otel = otel4sMessaging.MessagingOperationType(value)
    assertEquals(ours, otel)
  }

  test("MessagingOperationValue.Receive") {
    val ours = MessagingOperationTypeValue.Receive.value
    val otel = otel4sMessaging.MessagingOperationTypeValue.Receive.value
    assertEquals(ours, otel)
  }

  test("MessagingMessageId") {
    val value = "value"
    val ours = MessagingMessageId(value)
    val otel = otel4sMessaging.MessagingMessageId(value)
    assertEquals(ours, otel)
  }

  test("MessagingDestinationName") {
    val value = "value"
    val ours = MessagingDestinationName(value)
    val otel = otel4sMessaging.MessagingDestinationName(value)
    assertEquals(ours, otel)
  }

}
