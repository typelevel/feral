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
import io.circe.Json
import io.circe.literal._
import io.circe.scalajs._
import munit.CatsEffectSuite

import java.util.concurrent.atomic.AtomicInteger
import scala.scalajs.js

class IOLambdaJsSuite extends CatsEffectSuite {

  test("initializes handler once") {

    val allocationCounter = new AtomicInteger
    val invokeCounter = new AtomicInteger
    val lambda = new IOLambda[String, String] {
      def handler = Resource
        .eval(IO(allocationCounter.getAndIncrement()))
        .as(_.event.map(Some(_)) <* IO(invokeCounter.getAndIncrement()))
    }

    val chars = 'A' to 'Z'
    chars.toList.traverse { c =>
      IO.fromPromise(
        IO(lambda.handlerFn(c.toString, DummyContext.asInstanceOf[facade.Context])))
        .assertEquals(c.toString.asInstanceOf[js.UndefOr[js.Any]])
    } *> IO {
      assertEquals(allocationCounter.get(), 1)
      assertEquals(invokeCounter.get(), chars.length)
    }
  }

  test("reads input and writes output") {

    val input = json"""{ "foo": "bar" }"""
    val output = json"""{ "woozle": "heffalump" }"""

    val lambda = new IOLambda[Json, Json] {
      def handler = Resource.pure(_ => IO(Some(output)))
    }

    IO.fromPromise(
      IO(
        lambda.handlerFn(
          input.asJsAny,
          DummyContext.asInstanceOf[facade.Context]
        )
      )
    ).map(decodeJs[Json](_))
      .assertEquals(Right(output))
  }

  object DummyContext extends js.Object {
    def functionName: String = ""
    def functionVersion: String = ""
    def invokedFunctionArn: String = ""
    def memoryLimitInMB: String = "0"
    def awsRequestId: String = ""
    def logGroupName: String = ""
    def logStreamName: String = ""
    def identity: js.UndefOr[CognitoIdentity] = js.undefined
    def clientContext: js.UndefOr[ClientContext] = js.undefined
  }

}
