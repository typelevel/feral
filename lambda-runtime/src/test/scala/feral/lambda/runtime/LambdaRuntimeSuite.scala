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
package runtime

import cats.effect.IO
import cats.effect._
import cats.syntax.all._
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.client.Client

import java.{util => ju}
import scala.concurrent.duration.DurationInt

import api.ContainerError

class LambdaRuntimeSuite extends BaseRuntimeSuite {

  test("The runtime can process an event and pass the result to the invocation response url") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationId <- Deferred[IO, String]
      client = Client.fromHttpApp[IO](
        (testInvocationResponseRoute(eventualInvocationId) <+> defaultRoutes(
          invocationQuota)).orNotFound)
      handler = (inv: Invocation[IO, Json]) => inv.event.map(_.some)
      runtimeFiber <- LambdaRuntime[IO, Json, Json](client)(
        Resource.eval(handler.pure[IO])).start
      invocationId <- eventualInvocationId.get.timeout(2.seconds).guarantee(runtimeFiber.cancel)
    } yield assertEquals(invocationId, "testId")
  }

  test("A valid context and JSON event is passed to the handler function during invocation") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocation <- Deferred[IO, (Json, Context[IO])]
      client = Client.fromHttpApp[IO](defaultRoutes(invocationQuota).orNotFound)
      handler = (inv: Invocation[IO, Json]) =>
        inv.event.both(inv.context).flatMap(eventualInvocation.complete) >> none[Json].pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocation <- eventualInvocation.get.timeout(2.seconds).guarantee(runtimeFiber.cancel)
      (jsonEvent, context) = invocation
      expectedJson = Json.obj("eventField" -> "test".asJson)
    } yield {
      assert(clue(jsonEvent) eqv clue(expectedJson))
      assert(clue(context.clientContext).exists(_.client.appTitle == "test"))
      assertEquals(context.identity, Some(CognitoIdentity("test", "test")))
      assertEquals(context.functionName, "test")
      assertEquals(context.memoryLimitInMB, 144)
    }
  }

  test(
    "The runtime will call the initialization error url and raise an exception when the handler function cannot be acquired") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(0)
      eventualInitError <- Deferred[IO, Json]
      client = Client.fromHttpApp(
        (testInitErrorRoute(eventualInitError) <+> defaultRoutes(invocationQuota)).orNotFound)
      badHandlerResource = Resource.make[IO, Invocation[IO, Json] => IO[Option[Json]]](
        IO.raiseError(new Exception("Failure acquiring handler")))(_ => IO.unit)
      runtimeFiber <- LambdaRuntime(client)(badHandlerResource).start
      errorRequest <- eventualInitError.get.timeout(2.seconds)
      errorRequestNoStackTrace = lambdaErrorBodyJsonNoStackTrace(errorRequest)
      runtimeOutcome <- runtimeFiber.join.timeout(2.seconds)
    } yield {
      assert(
        errorRequestNoStackTrace.exists(_ eqv expectedErrorBody("Failure acquiring handler")))
      assert(runtimeOutcome.isError)
    }
  }

  test(
    "The runtime will call the invocation error url when the handler function errors during processing") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp(
        (testInvocationErrorRoute(eventualInvocationError) <+> defaultRoutes(
          invocationQuota)).orNotFound)
      handler = (_: Invocation[IO, Json]) => IO.raiseError[Option[Json]](new Exception("Error"))
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInvocationError.get.timeout(2.seconds)
      errorRequestNoStackTrace = lambdaErrorBodyJsonNoStackTrace(errorRequest)
      _ <- runtimeFiber.cancel
      outcome <- runtimeFiber.join
    } yield {
      assert(errorRequestNoStackTrace.exists(_ eqv expectedErrorBody()))
      assertEquals(outcome, Outcome.Canceled[IO, Throwable, INothing]())
    }
  }

  test(
    "The runtime will call the initialization error url when a needed environment variable is not available") {
    val lambdaFunctionNameEnvVar = LambdaRuntimeEnv.AWS_LAMBDA_FUNCTION_NAME
    implicit val env: LambdaRuntimeEnv[IO] =
      createTestEnv(funcName =
        IO.raiseError(new NoSuchElementException(lambdaFunctionNameEnvVar)))
    for {
      invocationQuota <- Ref[IO].of(0)
      eventualInitError <- Deferred[IO, Json]
      client = Client.fromHttpApp(
        (testInitErrorRoute(eventualInitError) <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Invocation[IO, Json]) => Json.obj().some.pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInitError.get.timeout(2.seconds)
      errorRequestNoStackTrace = lambdaErrorBodyJsonNoStackTrace(errorRequest)
      runtimeOutcome <- runtimeFiber.join.timeout(2.seconds)
    } yield {
      assert(
        errorRequestNoStackTrace.exists(
          _ eqv expectedErrorBody(lambdaFunctionNameEnvVar, "NoSuchElementException")))
      val Outcome.Errored(ex: ju.NoSuchElementException) = runtimeOutcome: @unchecked
      assertEquals(ex.getMessage(), "AWS_LAMBDA_FUNCTION_NAME")
    }
  }

  test("The runtime will crash when init error api returns the container error") {
    val lambdaFunctionNameEnvVar = LambdaRuntimeEnv.AWS_LAMBDA_FUNCTION_NAME
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv(funcName =
      IO.raiseError(new NoSuchElementException(lambdaFunctionNameEnvVar)))
    for {
      invocationQuota <- Ref[IO].of(0)
      eventualInitError <- Deferred[IO, Json]
      client = Client.fromHttpApp(
        (testInitContainerErrorRoute(eventualInitError) <+> defaultRoutes(
          invocationQuota)).orNotFound)
      handler = (_: Invocation[IO, Json]) => Json.obj().some.pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInitError.get.timeout(2.seconds)
      errorRequestNoStackTrace = lambdaErrorBodyJsonNoStackTrace(errorRequest)
      runtimeOutcome <- runtimeFiber.join.timeout(2.seconds)
    } yield {
      assert(
        errorRequestNoStackTrace.exists(
          _ eqv expectedErrorBody(lambdaFunctionNameEnvVar, "NoSuchElementException")))
      assertEquals(runtimeOutcome, Outcome.Errored[IO, Throwable, INothing](ContainerError))
    }

  }

  test(
    "The runtime will recover from a function error and continue processing next invocation") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(2)
      eventualInvocationError <- Deferred[IO, Json]
      eventualResponse <- Deferred[IO, String]
      call <- Ref[IO].of(0)
      client = Client.fromHttpApp(
        (testInvocationErrorRoute(eventualInvocationError)
          <+> testInvocationResponseRoute(eventualResponse)
          <+> defaultRoutes(invocationQuota)).orNotFound
      )
      handler = (_: Invocation[IO, Json]) =>
        for {
          call <- call.getAndUpdate(_ + 1)
          resp <-
            if (call === 0) IO.raiseError(new Exception("First invocation error"))
            else Json.obj().some.pure[IO]
        } yield resp
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      invocationErrorNoStackTrace = lambdaErrorBodyJsonNoStackTrace(invocationError)
      secondInvocationResponse <- eventualResponse.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield {
      assert(
        invocationErrorNoStackTrace.exists(_ eqv expectedErrorBody("First invocation error")))
      assertEquals(secondInvocationResponse, "testId")
    }
  }
  test("The runtime will crash when response api returns the container error") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationId <- Deferred[IO, String]
      client = Client.fromHttpApp(
        (testInvocationResponseContainerErrorRoute(eventualInvocationId) <+> defaultRoutes(
          invocationQuota)).orNotFound)
      handler = (_: Invocation[IO, Json]) => Json.obj().some.pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInvocationId.get.timeout(2.seconds)
      runtimeOutcome <- runtimeFiber.join.timeout(2.seconds)
    } yield {
      assertEquals(errorRequest, "testId")

      assertEquals(runtimeOutcome, Outcome.Errored[IO, Throwable, INothing](ContainerError))
    }
  }

}
