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
import cats.effect.Ref
import cats.effect.kernel.Resource
import cats.syntax.all._
import io.circe.scalajs._
import munit.CatsEffectSuite

import scala.scalajs.js
import scala.scalajs.js.|

class ExportedLambdaSuite extends CatsEffectSuite {
  test("exported lambda") {
    val context = DummyContext.asInstanceOf[facade.Context]

    for {
      allocationCounter <- IO.ref(0)
      invokeCounter <- IO.ref(0)
      lambda = new CountingIOLambda(allocationCounter, invokeCounter)

      _ <- ('0' to 'z')
        .map(_.toString)
        .toList
        .traverse(x =>
          IO.fromPromise(IO(lambda.impl(x, context)))
            .assertEquals(x.asJsAny.asInstanceOf[js.Any | Unit]))

      _ <- allocationCounter.get.assertEquals(1)
      _ <- invokeCounter.get.assertEquals(75)
    } yield ()

  }
}

class CountingIOLambda(allocationCounter: Ref[IO, Int], invokeCounter: Ref[IO, Int])
    extends IOLambda[String, String] {

  override def handler: Resource[IO, LambdaEnv[IO, String] => IO[Option[String]]] =
    Resource
      .eval(allocationCounter.getAndUpdate(_ + 1))
      .as(_.event.map(Some(_)) <* invokeCounter.getAndUpdate(_ + 1))

  // This would be exported with `@JSExportTopLevel("handler")`
  def impl: HandlerFn = handlerFn
}

object DummyContext extends js.Object {
  def callbackWaitsForEmptyEventLoop: Boolean = true
  def functionName: String = ""
  def functionVersion: String = ""
  def invokedFunctionArn: String = ""
  def memoryLimitInMB: String = "512"
  def awsRequestId: String = ""
  def logGroupName: String = ""
  def logStreamName: String = ""
  def identity: js.UndefOr[CognitoIdentity] = js.undefined
  def clientContext: js.UndefOr[ClientContext] = js.undefined
  def getRemainingTimeInMillis(): Double = 0
}
