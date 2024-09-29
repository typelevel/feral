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

package feral.lambda
package otel4s

import cats.effect.IO
import cats.syntax.all._
import io.circe.Decoder
import io.circe.Encoder
import munit.CatsEffectSuite
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes.CloudProviderValue
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes
import org.typelevel.otel4s.trace.SpanKind

import java.util.concurrent.atomic.AtomicInteger

class SharedTracedHandlerSuite extends CatsEffectSuite {
  import SharedTracedHandlerSuite._

  val fixture = ResourceFixture(TracesTestkit.inMemory[IO]())

  fixture.test("single root span is created for single invocation") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val event = TestEvent("1", "body")

      val attributes = Attributes(
        FaasExperimentalAttributes.FaasName(functionName),
        FaasExperimentalAttributes.FaasVersion(functionVersion),
        FaasExperimentalAttributes.FaasInstance(logStreamName),
        FaasExperimentalAttributes.FaasMaxMemory(memoryLimitInMB * 1024L * 1024L),
        CloudExperimentalAttributes.CloudProvider(CloudProviderValue.Aws.value),
        CloudExperimentalAttributes.CloudResourceId(invokedFunctionArn)
      )

      implicit val inv: Invocation[IO, TestEvent] = makeInvocation(event)

      val invokeCounter = new AtomicInteger

      val tracedHandler = TracedHandler[IO, TestEvent, INothing] {
        for {
          _ <- IO(invokeCounter.getAndIncrement())
        } yield Option.empty[INothing]
      }

      for {
        _ <- tracedHandler
        spans <- traces.finishedSpans
        headSpan = spans.headOption
      } yield {
        assertEquals(spans.length, 1)
        assertEquals(headSpan.map(_.name), Some(functionName))
        assertEquals(headSpan.map(_.attributes.elements), Some(attributes))
        assertEquals(invokeCounter.get(), 1)
      }

    }
  }

  fixture.test("multiple root spans created for multiple invocations") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val event = TestEvent("2", "body")

      val attributes = Attributes(
        FaasExperimentalAttributes.FaasName(functionName),
        FaasExperimentalAttributes.FaasVersion(functionVersion),
        FaasExperimentalAttributes.FaasInstance(logStreamName),
        FaasExperimentalAttributes.FaasMaxMemory(memoryLimitInMB * 1024L * 1024L),
        CloudExperimentalAttributes.CloudProvider(CloudProviderValue.Aws.value),
        CloudExperimentalAttributes.CloudResourceId(invokedFunctionArn)
      )

      implicit val inv: Invocation[IO, TestEvent] = makeInvocation(event)

      val invokeCounter = new AtomicInteger

      val tracedHandler = TracedHandler[IO, TestEvent, INothing] {
        for {
          _ <- IO(invokeCounter.getAndIncrement())
        } yield Option.empty[INothing]
      }

      val handlerIndices = 0.to(4)
      val runHandlers = handlerIndices.toList.map(_ => tracedHandler).sequence

      for {
        _ <- runHandlers
        spans <- traces.finishedSpans
      } yield {
        assertEquals(spans.length, handlerIndices.size)
        spans.map(s => assertEquals(s.name, functionName))
        spans.map(s => assertEquals(s.attributes.elements, attributes))
        assertEquals(invokeCounter.get(), handlerIndices.size)
      }
    }
  }
}

object SharedTracedHandlerSuite {

  case class TestEvent(traceId: String, payload: String)

  object TestEvent {

    implicit val decoder: Decoder[TestEvent] =
      Decoder.forProduct2("traceId", "payload")(TestEvent.apply)
    implicit val encoder: Encoder[TestEvent] =
      Encoder.forProduct2("traceId", "payload")(ev => (ev.traceId, ev.payload))

    implicit val attr: EventSpanAttributes[TestEvent] =
      new EventSpanAttributes[TestEvent] {

        override def contextCarrier(e: TestEvent): Map[String, String] =
          Map("trace_id" -> e.traceId)

        override def spanKind: SpanKind = SpanKind.Consumer

        override def attributes(e: TestEvent): Attributes = Attributes.empty

      }
  }

  val memoryLimitInMB = 1024
  val functionVersion = "1.0.1"
  val logStreamNameArn =
    "arn:aws:logs:us-east-1:123456789012:log-group:/aws/lambda/my-function:*"
  val invokedFunctionArn =
    "arn:aws:lambda:us-west-2:123456789012:function:test-function-name:PROD"
  val functionName = "test-function-name"
  val logStreamName = "log-stream-name"
  val logGroupName = "log-group-name"

  val context: Context[IO] = {
    Context.apply[IO](
      functionName = functionName,
      functionVersion = functionVersion,
      invokedFunctionArn = invokedFunctionArn,
      memoryLimitInMB = memoryLimitInMB,
      awsRequestId = "aws-request-id",
      logGroupName = logGroupName,
      logStreamName = logStreamName,
      identity = None,
      clientContext = None,
      remainingTime = IO.realTime
    )
  }

  def makeInvocation(event: TestEvent): Invocation[IO, TestEvent] =
    Invocation.pure[IO, TestEvent](event, context)
}
