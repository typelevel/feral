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

import java.util.concurrent.atomic.AtomicInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class IOLambdaJvmSuite extends FunSuite {

  test("initializes handler once") {

    val allocationCounter = new AtomicInteger
    val invokeCounter = new AtomicInteger
    val lambda = new IOLambda[String, String] {
      def handler = Resource
        .eval(IO(allocationCounter.getAndIncrement()))
        .as(_.event.map(Some(_)) <* IO(invokeCounter.getAndIncrement()))
    }

    val chars = 'A' to 'Z'
    chars.foreach { c =>
      val os = new ByteArrayOutputStream

      val json = s""""$c""""
      lambda.handleRequest(
        new ByteArrayInputStream(json.getBytes()),
        os,
        DummyContext
      )

      assertEquals(new String(os.toByteArray()), json)
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

    val os = new ByteArrayOutputStream

    lambda.handleRequest(
      new ByteArrayInputStream(input.toString.getBytes()),
      os,
      DummyContext
    )

    assertEquals(
      jawn.parseByteArray(os.toByteArray()),
      Right(output),
      new String(os.toByteArray())
    )
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
