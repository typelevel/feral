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
import munit.CatsEffectSuite
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes
import org.typelevel.otel4s.trace.SpanKind

import java.util.concurrent.atomic.AtomicInteger
import scala.scalajs.js

class TracedHandlerSuite extends CatsEffectSuite {
  import TracedHandlerSuite._

  val fixture = ResourceFixture(TracesTestkit.inMemory[IO]())

  fixture.test("single root span is created for single invocation") { traces =>
    traces.tracerProvider.tracer("test-tracer").get.flatMap { implicit tracer =>
      val allocationCounter = new AtomicInteger
      val invokeCounter = new AtomicInteger

      val lambda = new IOLambda[String, String] {
        def handler =
          Resource.eval(IO(allocationCounter.getAndIncrement())).as { implicit inv =>
            def fn(ev: String): IO[Option[String]] =
              for {
                _ <- IO(invokeCounter.getAndIncrement())
                res = Some(ev)
              } yield res

            TracedHandler(fn)
          }
      }

      val event = "event"

      val functionName = "test-function-name"
      val run = IO.fromPromise(IO(lambda.handlerFn(event, new DummyContext(functionName))))

      val attributeKeys = Set[AttributeKey[_]](
        FaasExperimentalAttributes.FaasName,
        FaasExperimentalAttributes.FaasVersion,
        FaasExperimentalAttributes.FaasInstance,
        FaasExperimentalAttributes.FaasMaxMemory,
        CloudExperimentalAttributes.CloudProvider,
        CloudExperimentalAttributes.CloudResourceId
      )

      for {
        res <- run
        spans <- traces.finishedSpans
        _ <- IO {
          assertEquals(spans.length, 1)
          assertEquals(spans.headOption.map(_.name), Some(functionName))
          assertEquals(spans.headOption.map(_.attributes.map(_.key).toSet), Some(attributeKeys))
          assertEquals(allocationCounter.get(), 1)
          assertEquals(invokeCounter.get(), 1)
          assertEquals(res, event.asInstanceOf[js.UndefOr[js.Any]])
        }
      } yield ()
    }
  }

  class DummyContext(fnName: String) extends facade.Context {
    def functionName = fnName
    def functionVersion = ""
    def invokedFunctionArn = ""
    def memoryLimitInMB = "0"
    def awsRequestId = ""
    def logGroupName = ""
    def logStreamName = ""
    def identity = js.undefined
    def clientContext = js.undefined
    def getRemainingTimeInMillis(): Double = 0
  }

}

object TracedHandlerSuite {

  implicit val attr: EventSpanAttributes[String] =
    EventSpanAttributes.empty[String](SpanKind.Internal)

}
