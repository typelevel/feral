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

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.amazonaws.services.lambda.runtime
import io.circe.Json
import io.circe.jawn
import io.circe.literal._
import munit.FunSuite

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class IOLambdaJvmSuite extends FunSuite {

  implicit class HandleOps[A, B](lambda: IOLambda[A, B]) {
    def handleRequestHelper(in: String): String = {
      val os = new ByteArrayOutputStream
      lambda.handleRequest(
        new ByteArrayInputStream(in.getBytes()),
        os,
        DummyContext
      )
      new String(os.toByteArray())
    }
  }

  test("initializes handler once during construction") {

    val allocationCounter = new AtomicInteger
    val invokeCounter = new AtomicInteger
    val lambda = new IOLambda[String, String] {
      def handler = Resource
        .eval(IO(allocationCounter.getAndIncrement()))
        .as(_.event.map(Some(_)) <* IO(invokeCounter.getAndIncrement()))
    }

    assertEquals(allocationCounter.get(), 1)

    val chars = 'A' to 'Z'
    chars.foreach { c =>
      val json = s""""$c""""
      assertEquals(lambda.handleRequestHelper(json), json)
    }

    assertEquals(allocationCounter.get(), 1)
    assertEquals(invokeCounter.get(), chars.length)
  }

  test("reads input and writes output") {

    val input = json"""{ "foo": "bar" }"""
    val output = json"""{ "woozle": "heffalump" }"""

    val lambda = new IOLambda[Json, Json] {
      def handler = Resource.pure(_ => IO(Some(output)))
    }

    assertEquals(
      jawn.parse(lambda.handleRequestHelper(input.noSpaces)),
      Right(output)
    )
  }

  test("gracefully handles broken initialization due to `val`") {

    def go(mkLambda: AtomicInteger => IOLambda[Unit, Unit]): Unit = {
      val counter = new AtomicInteger
      val lambda = mkLambda(counter)
      assertEquals(counter.get(), 0) // init failed
      lambda.handleRequestHelper("{}")
      assertEquals(counter.get(), 1) // inited
      lambda.handleRequestHelper("{}")
      assertEquals(counter.get(), 1) // did not re-init
    }

    go { counter =>
      new IOLambda[Unit, Unit] {
        val handler = Resource.eval(IO(counter.getAndIncrement())).as(_ => IO(None))
      }
    }

    go { counter =>
      new IOLambda[Unit, Unit] {
        def handler = resource.as(_ => IO(None))
        val resource = Resource.eval(IO(counter.getAndIncrement()))
      }
    }
  }

  object DummyContext extends runtime.Context {
    override def getAwsRequestId(): String = ""
    override def getLogGroupName(): String = ""
    override def getLogStreamName(): String = ""
    override def getFunctionName(): String = ""
    override def getFunctionVersion(): String = ""
    override def getInvokedFunctionArn(): String = ""
    override def getIdentity(): runtime.CognitoIdentity = null
    override def getClientContext(): runtime.ClientContext = null
    override def getRemainingTimeInMillis(): Int = Int.MaxValue
    override def getMemoryLimitInMB(): Int = 0
    override def getLogger(): runtime.LambdaLogger = null
  }

}
