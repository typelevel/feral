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
import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.DynamoDbRecord
import feral.lambda.events.DynamoDbStreamEvent
import feral.lambda.events.SqsEvent
import feral.lambda.events.SqsRecord
import feral.lambda.events.SqsRecordAttributes
import io.circe.Decoder
import io.circe.Encoder
import munit.CatsEffectSuite
import org.typelevel.ci._
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes.CloudProviderValue
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes
import org.typelevel.otel4s.trace.SpanKind

import java.util.concurrent.atomic.AtomicInteger

class SharedTracedHandlerSuite extends CatsEffectSuite {
  import SharedTracedHandlerSuite._

  val fixture = ResourceFunFixture(TracesTestkit.inMemory[IO]())

  fixture.test("single root span is created for single invocation") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val event = TestEvent("1", "body")

      val attributes = expectedContextAttributes

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

      val attributes = expectedContextAttributes

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

  fixture.test("api gateway invocation uses route span name and route attributes") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val event = ApiGatewayProxyEvent(
        body = Some(""),
        resource = "/pets/{petId}",
        path = "/pets/10",
        httpMethod = "GET",
        isBase64Encoded = false,
        queryStringParameters = None,
        multiValueQueryStringParameters = None,
        pathParameters = None,
        stageVariables = None,
        headers = Some(Map(ci"x-forwarded-proto" -> "https")),
        multiValueHeaders = None
      )

      implicit val inv: Invocation[IO, ApiGatewayProxyEvent] =
        Invocation.pure[IO, ApiGatewayProxyEvent](event, context)

      val tracedHandler = TracedHandler[IO, ApiGatewayProxyEvent, INothing](IO.pure(None))

      for {
        _ <- tracedHandler
        spans <- traces.finishedSpans
      } yield {
        val attributes = expectedContextAttributes ++ Attributes(
          FaasExperimentalAttributes.FaasTrigger(
            FaasExperimentalAttributes.FaasTriggerValue.Http.value),
          HttpRoute("/pets/{petId}")
        )

        assertEquals(spans.length, 1)
        assertEquals(spans.head.name, "/pets/{petId}")
        assertEquals(spans.head.attributes.elements, attributes)
      }
    }
  }

  fixture.test("sqs invocation uses queue span name and process messaging attributes") {
    traces =>
      traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
        val record = SqsRecord(
          messageId = "message-1",
          receiptHandle = "receipt",
          body = "payload",
          attributes = SqsRecordAttributes(
            awsTraceHeader = None,
            approximateReceiveCount = "1",
            sentTimestamp = java.time.Instant.EPOCH,
            senderId = "sender",
            approximateFirstReceiveTimestamp = java.time.Instant.EPOCH,
            sequenceNumber = None,
            messageGroupId = None,
            messageDeduplicationId = None
          ),
          messageAttributes = Map.empty,
          md5OfBody = "md5",
          eventSource = "aws:sqs",
          eventSourceArn = "arn:aws:sqs:us-east-1:123456789012:queue-1",
          awsRegion = "us-east-1"
        )

        val event = SqsEvent(List(record))

        implicit val inv: Invocation[IO, SqsEvent] =
          Invocation.pure[IO, SqsEvent](event, context)

        val tracedHandler = TracedHandler[IO, SqsEvent, INothing](IO.pure(None))

        for {
          _ <- tracedHandler
          spans <- traces.finishedSpans
        } yield {
          val attributes = expectedContextAttributes ++ Attributes(
            FaasExperimentalAttributes.FaasTrigger(
              FaasExperimentalAttributes.FaasTriggerValue.Pubsub.value),
            MessagingSystem("aws_sqs"),
            MessagingOperationType("process"),
            MessagingDestinationName("queue-1")
          )

          assertEquals(spans.length, 1)
          assertEquals(spans.head.name, "queue-1 process")
          assertEquals(spans.head.attributes.elements, attributes)
        }
      }
  }

  fixture.test("dynamodb invocation uses datasource trigger") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val record = DynamoDbRecord(
        awsRegion = Some("us-east-1"),
        dynamodb = None,
        eventId = Some("event-1"),
        eventName = Some("INSERT"),
        eventSource = Some("aws:dynamodb"),
        eventSourceArn = Some(
          "arn:aws:dynamodb:us-east-1:123456789012:table/orders/stream/2026-04-20T00:00:00.000"
        ),
        eventVersion = Some("1.1"),
        userIdentity = None
      )

      val event = DynamoDbStreamEvent(List(record))

      implicit val inv: Invocation[IO, DynamoDbStreamEvent] =
        Invocation.pure[IO, DynamoDbStreamEvent](event, context)

      val tracedHandler = TracedHandler[IO, DynamoDbStreamEvent, INothing](IO.pure(None))

      for {
        _ <- tracedHandler
        spans <- traces.finishedSpans
      } yield {
        val attributes = expectedContextAttributes ++ Attributes(
          FaasExperimentalAttributes.FaasTrigger(
            FaasExperimentalAttributes.FaasTriggerValue.Datasource.value)
        )

        assertEquals(spans.length, 1)
        assertEquals(spans.head.name, functionName)
        assertEquals(spans.head.attributes.elements, attributes)
      }
    }
  }
}

object SharedTracedHandlerSuite {
  private val CloudAccountId = AttributeKey.string("cloud.account.id")
  private val CloudRegion = AttributeKey.string("cloud.region")
  private val AwsLambdaInvokedArn = AttributeKey.string("aws.lambda.invoked_arn")
  private val HttpRoute = AttributeKey.string("http.route")
  private val MessagingSystem = AttributeKey.string("messaging.system")
  private val MessagingOperationType = AttributeKey.string("messaging.operation.type")
  private val MessagingDestinationName = AttributeKey.string("messaging.destination.name")

  case class TestEvent(traceId: String, payload: String)

  object TestEvent {

    implicit val decoder: Decoder[TestEvent] =
      Decoder.forProduct2("traceId", "payload")(TestEvent.apply)
    implicit val encoder: Encoder[TestEvent] =
      Encoder.forProduct2("traceId", "payload")(ev => (ev.traceId, ev.payload))

    implicit val attr: EventAttributeSource[TestEvent] =
      new EventAttributeSource[TestEvent] {

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
  val cloudResourceId =
    "arn:aws:lambda:us-west-2:123456789012:function:test-function-name:1.0.1"
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

  val expectedContextAttributes: Attributes = Attributes(
    FaasExperimentalAttributes.FaasInvocationId("aws-request-id"),
    FaasExperimentalAttributes.FaasName(functionName),
    FaasExperimentalAttributes.FaasVersion(functionVersion),
    FaasExperimentalAttributes.FaasInstance(logStreamName),
    FaasExperimentalAttributes.FaasMaxMemory(memoryLimitInMB * 1024L * 1024L),
    CloudExperimentalAttributes.CloudProvider(CloudProviderValue.Aws.value),
    CloudExperimentalAttributes.CloudResourceId(cloudResourceId),
    CloudAccountId("123456789012"),
    CloudRegion("us-west-2"),
    AwsLambdaInvokedArn(invokedFunctionArn)
  )
}
