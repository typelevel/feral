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

package feral.googlecloud

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import com.google.events.cloud.pubsub.v1.MessagePublishedData
import com.google.events.cloud.pubsub.v1.PubsubMessage
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import io.circe.Json
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

class IOCloudEventFunctionSuite extends munit.FunSuite {
  implicit class AcceptOps[A, B](function: IOCloudEventsFunction[A, B]) {
    def acceptFunctionHelper(event: CloudEvent): String = {
      function.accept(event)
      new String(event.getData().toBytes(), StandardCharsets.UTF_8)
    }
  }

  test("initializes handler once during construction") {
    val allocationCounter = new AtomicInteger
    val invokeCounter = new AtomicInteger
    val function = new IOCloudEventsFunction[Json, Unit] {
      def handler = Resource
        .eval(IO(allocationCounter.getAndIncrement()))
        .as(ev => IO.pure(ev.data) <* IO(invokeCounter.getAndIncrement()) >> IO.unit)
    }

    assertEquals(allocationCounter.get(), 1)

    val msgs = ('A' to 'Z').zip(1 to 26).map(elem => s"${elem._1}${elem._2}")

    msgs.foreach { elem =>
      val dummyEvent = DummyCloudEvent(elem)
      val dummyEventOutput = new String(dummyEvent.getData().toBytes(), StandardCharsets.UTF_8)
      val functionOutput = function.acceptFunctionHelper(dummyEvent)
      assertEquals(dummyEventOutput, functionOutput)
    }

    assertEquals(allocationCounter.get(), 1)
    assertEquals(invokeCounter.get(), msgs.length)
  }

  test("reads input and writes output") {

    implicit val ioRuntime: IORuntime = IORuntime.global

    val firstEvent = DummyCloudEvent("first event")
    val secondEvent = DummyCloudEvent("second event")
    val processed_events = IO.ref(List.empty[CloudEvent]).unsafeRunSync()

    val function = new IOCloudEventsFunction[Json, Unit] {
      def handler = Resource.pure(_ =>
        IO(
          processed_events
            .update(lis => lis ::: List(firstEvent, secondEvent))
            .unsafeRunSync()(ioRuntime)))
    }

    val firstOutput = function.acceptFunctionHelper(firstEvent)
    val secondOutput = function.acceptFunctionHelper(secondEvent)
    val listOfProcessedEvents = processed_events.get.unsafeRunSync()

    val firstEventOutput = new String(
      listOfProcessedEvents.head.getData().toBytes(),
      StandardCharsets.UTF_8
    )
    val secondEventOutput = new String(
      listOfProcessedEvents.last.getData().toBytes(),
      StandardCharsets.UTF_8
    )

    assertEquals(firstOutput, firstEventOutput)
    assertEquals(secondOutput, secondEventOutput)

  }

  test("gracefully handles broken initialization due to `val`") {

    def runFunction(mkFunction: AtomicInteger => IOCloudEventsFunction[Json, Unit]): Unit = {
      val counter = new AtomicInteger
      val function = mkFunction(counter)
      val dummyEvent = DummyCloudEvent("dummy")

      assertEquals(counter.get(), 0)
      function.acceptFunctionHelper(dummyEvent)
      assertEquals(counter.get(), 1)
      function.acceptFunctionHelper(dummyEvent)
      assertEquals(counter.get(), 1)

    }

    runFunction { counter =>
      new IOCloudEventsFunction[Json, Unit] {
        val handler = Resource.eval(IO(counter.getAndIncrement())).as(_ => IO.unit)
      }
    }

    runFunction { counter =>
      new IOCloudEventsFunction[Json, Unit] {
        def handler = resource.as(_ => IO.unit)
        val resource = Resource.eval(IO(counter.getAndIncrement()))
      }
    }

  }

  object DummyCloudEvent {

    private def generateEvent(in: String = ""): CloudEvent = {
      var msg = "Feral"

      if (in.nonEmpty) {
        msg = in
      }

      val message = PubsubMessage
        .newBuilder()
        .setData(ByteString.copyFromUtf8(msg))
        .putAttributes("attr1", "attr1-value")
        .setMessageId("message-id")
        .setPublishTime(Timestamps.parse("2021-02-05T04:06:14.109Z"))
        .build()

      val data = MessagePublishedData.newBuilder().setMessage(message).build()

      val json_formatted_data = JsonFormat.printer().print(data)

      val event = CloudEventBuilder
        .v1()
        .withId("1234-5678-9012-3456")
        .withType("pubsub.message")
        .withSource(java.net.URI.create("https://github.com/cloudevents/spec/pull/123"))
        .withData(json_formatted_data.getBytes())
        .build()

      event
    }

    def apply(): CloudEvent = generateEvent()
    def apply(in: String): CloudEvent = generateEvent(in)

  }
}
