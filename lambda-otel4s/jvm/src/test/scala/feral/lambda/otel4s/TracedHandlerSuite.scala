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
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.amazonaws.services.lambda.runtime
import io.circe.Decoder
import io.circe.Encoder
import io.circe.syntax._
import munit.CatsEffectSuite
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.typelevel.otel4s.trace.SpanKind

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class TracedHandlerSuite extends CatsEffectSuite {
  import TracedHandlerSuite._

  implicit class HandleOps[A, B](lambda: IOLambda[A, B]) {
    def handleRequestHelper(in: String, ctx: runtime.Context): String = {
      val os = new ByteArrayOutputStream
      lambda.handleRequest(
        new ByteArrayInputStream(in.getBytes()),
        os,
        ctx
      )
      new String(os.toByteArray())
    }
  }

  val fixture = ResourceFixture(TracesTestkit.inMemory[IO]())

  fixture.test("single root span is created for single invocation") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val allocationCounter = new AtomicInteger
      val invokeCounter = new AtomicInteger

      val lambda = new IOLambda[TestEvent, String] {
        def handler =
          Resource.eval(IO(allocationCounter.getAndIncrement())).as { implicit inv =>
            def fn(ev: TestEvent): IO[Option[String]] =
              for {
                _ <- IO(invokeCounter.getAndIncrement())
                res = Some(ev.payload)
              } yield res

            TracedHandler(fn)
          }
      }

      val event = TestEvent("1", "body")
      val payload = event.asJson.noSpaces

      val functionName = "test-function-name"
      val run = IO(lambda.handleRequestHelper(payload, DummyContext(functionName)))

      for {
        res <- run
        spans <- traces.finishedSpans
        _ <- IO {
          assertEquals(res, "\"body\"".toString)
          assertEquals(spans.length, 1)
          assertEquals(spans.headOption.map(_.name), Some(functionName))
        }
      } yield ()
    }

  }

  fixture.test("multiple root span per invocation created with function name ") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val allocationCounter = new AtomicInteger
      val invokeCounter = new AtomicInteger

      val lambda = new IOLambda[TestEvent, String] {
        def handler =
          Resource.eval(IO(allocationCounter.getAndIncrement())).as { implicit inv =>
            def fn(ev: TestEvent): IO[Option[String]] =
              for {
                _ <- IO(invokeCounter.getAndIncrement())
                res = Some(ev.payload)
              } yield res

            TracedHandler(fn)
          }
      }

      val functionName = "test-function-name"
      val chars = 'A'.to('C').toList
      val run =
        chars.zipWithIndex.map { case (c, i) => TestEvent(i.toString, c.toString) }.traverse {
          e =>
            val payload = e.asJson.noSpaces
            IO(lambda.handleRequestHelper(payload, DummyContext(functionName)))
        }

      val expectedSpanNames = List.fill(3)(functionName)

      for {
        res <- run
        spans <- traces.finishedSpans
        _ <- IO {
          assertEquals(res.length, chars.length)
          assertEquals(spans.length, chars.length)
          assertEquals(spans.map(_.name), expectedSpanNames)
        }
      } yield ()
    }
  }

  case class DummyContext(functionName: String) extends runtime.Context {
    override def getAwsRequestId(): String = ""
    override def getLogGroupName(): String = ""
    override def getLogStreamName(): String = ""
    override def getFunctionName(): String = functionName
    override def getFunctionVersion(): String = ""
    override def getInvokedFunctionArn(): String = ""
    override def getIdentity(): runtime.CognitoIdentity = null
    override def getClientContext(): runtime.ClientContext = null
    override def getRemainingTimeInMillis(): Int = Int.MaxValue
    override def getMemoryLimitInMB(): Int = 0
    override def getLogger(): runtime.LambdaLogger = null
  }

}

object TracedHandlerSuite {

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

        override def attributes(e: TestEvent): List[Attribute[_]] = List.empty

      }
  }

  def tracedLambda(allocationCounter: AtomicInteger, invokeCounter: AtomicInteger) = {}

}
