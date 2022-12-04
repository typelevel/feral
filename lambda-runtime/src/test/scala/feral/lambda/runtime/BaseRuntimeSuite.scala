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

package feral.lambda.runtime

import munit.CatsEffectSuite
import cats.effect.IO
import feral.lambda._
import io.circe._
import cats.effect._
import io.circe.syntax.EncoderOps
import org.http4s.Method.{GET, POST}
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.http4s.Uri.Path.Root
import org.http4s.syntax.all._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe._
import feral.lambda.runtime.headers._

abstract class BaseRuntimeSuite extends CatsEffectSuite {

  implicit val jsonEncoder: EntityEncoder[IO, Json] =
    jsonEncoderWithPrinter[IO](Printer.noSpaces.copy(dropNullValues = true))

  def createTestEnv(
      funcName: IO[String] = IO("test"),
      memorySize: IO[Int] = IO(144),
      funcVersion: IO[String] = IO("1.0"),
      logGroupName: IO[String] = IO("test"),
      logStreamName: IO[String] = IO("test"),
      runtimeApi: IO[Uri] = IO(uri"testApi")): LambdaRuntimeEnv[IO] = new LambdaRuntimeEnv[IO] {

    def lambdaFunctionName: IO[String] = funcName

    def lambdaFunctionMemorySize: IO[Int] = memorySize

    def lambdaFunctionVersion: IO[String] = funcVersion

    def lambdaLogGroupName: IO[String] = logGroupName

    def lambdaLogStreamName: IO[String] = logStreamName

    def lambdaRuntimeApi: IO[Uri] = runtimeApi
  }

  def defaultRoutes(invocationQuota: Ref[IO, Int]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "invocation" / "next" =>
        for {
          currentInvocations <- invocationQuota.getAndUpdate(_ - 1)
          _ <- if (currentInvocations > 0) IO.unit else IO.never
          headers = Headers(
            `Lambda-Runtime-Aws-Request-Id`("testId"),
            `Lambda-Runtime-Deadline-Ms`(20),
            `Lambda-Runtime-Invoked-Function-Arn`("test"),
            `Lambda-Runtime-Client-Context`(
              new ClientContext(
                new ClientContextClient(
                  "test",
                  "test",
                  "test",
                  "test",
                  "test"
                ),
                new ClientContextEnv(
                  "test",
                  "test",
                  "test",
                  "test",
                  "test"
                ),
                JsonObject.empty
              )),
            `Lambda-Runtime-Client-Identity`(new CognitoIdentity("test", "test"))
          )
          body = Json.obj("eventField" -> "test".asJson)
          resp <- Ok(body, headers)
        } yield resp
      case POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "response" =>
        Ok()
      case POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "error" =>
        Ok()
      case POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "init" / "error" =>
        Ok()
    }

  def testInvocationErrorRoute(eventualInvocationError: Deferred[IO, Json]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "error" =>
        req.as[Json].flatTap(eventualInvocationError.complete) *> Ok()
    }

  def testInvocationResponseRoute(eventualInvocationId: Deferred[IO, String]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" =>
        eventualInvocationId.complete(id) >> Ok()
    }

  def testInitErrorRoute(eventualInitError: Deferred[IO, Json]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "testApi" / LambdaRuntime.ApiVersion / "runtime" / "init" / "error" =>
        req.as[Json].flatTap(eventualInitError.complete(_)) *> Ok()
    }

  def expectedErrorBody(errorMessage: String = "Error", errorType: String = "Exception"): Json =
    Json.obj(
      "errorMessage" -> errorMessage.asJson,
      "errorType" -> errorType.asJson,
      "stackTrace" -> List[String]().asJson
    )
}
