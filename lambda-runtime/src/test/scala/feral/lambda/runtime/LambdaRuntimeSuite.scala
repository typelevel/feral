package feral.lambda.runtime

import cats.effect.IO
import feral.lambda.Context
import io.circe.Json
import cats.syntax.all._
import cats.effect._
import io.circe.syntax.EncoderOps
import munit._
import org.http4s.client.Client
import scala.concurrent.duration.DurationInt

class LambdaRuntimeSuite extends BaseRuntimeSuite {

  test("The runtime can process an event and pass the result to the invocation response url") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationId <- Deferred[IO, String]
      client = Client.fromHttpApp[IO]((testInvocationResponseRoute(eventualInvocationId) <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => Json.obj().pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationId <- eventualInvocationId.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield assert(invocationId == "testId")
  }

  test("A valid context and JSON event is passed to the handler function during invocation") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocation <- Deferred[IO, (Json, Context[IO])]
      client = Client.fromHttpApp[IO](defaultRoutes(invocationQuota).orNotFound)
      handler = (json: Json, context: Context[IO]) => eventualInvocation.complete((json, context)) >> Json.obj().pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      (jsonEvent, context) <- eventualInvocation.get.timeout(2.seconds)
      expectedJson = Json.obj("eventField" -> "test".asJson)
      _ <- runtimeFiber.cancel
    } yield {
      assert(jsonEvent eqv expectedJson)
      assert(context.clientContext.exists(_.client.appTitle == "test"))
      assert(context.identity.exists(_.identityId == "test"))
      assert(context.functionName == "test")
      assert(context.memoryLimitInMB == 144)
    }
  }

  test("The runtime will call the initialization error url when the handler function cannot be acquired") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(0)
      eventualInitError <- Deferred[IO, Json]
      client = Client.fromHttpApp((testInitErrorRoute(eventualInitError) <+> defaultRoutes(invocationQuota)).orNotFound)
      badHandlerResource = Resource.make[IO, (Json, Context[IO]) => IO[Json]](IO.raiseError(new Exception("Failure acquiring handler")))(_ => IO.unit)
      runtimeFiber <- LambdaRuntime(client)(badHandlerResource).start
      errorRequest <- eventualInitError.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield assert(errorRequest eqv expectedErrorBody("Failure acquiring handler"))
  }

  test("The runtime will call the invocation error url when the handler function errors during processing") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((testInvocationErrorRoute(eventualInvocationError) <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => IO.raiseError[Json](new Exception("Error"))
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInvocationError.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield assert(errorRequest eqv expectedErrorBody())
  }

  test("The runtime will call the invocation error url when a needed environment variable is not available") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv(funcName = IO.raiseError(new Exception("Error")))
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((testInvocationErrorRoute(eventualInvocationError) <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => Json.obj().pure[IO]
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      errorRequest <- eventualInvocationError.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield assert(errorRequest eqv expectedErrorBody())
  }

  test("The runtime will continue to process events even after encountering an invocation error") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(2)
      eventualInvocationError <- Deferred[IO, Json]
      eventualResponse <- Deferred[IO, String]
      client = Client.fromHttpApp(
        (testInvocationErrorRoute(eventualInvocationError)
        <+> testInvocationResponseRoute(eventualResponse)
        <+> defaultRoutes(invocationQuota)).orNotFound
      )
      handler = (_: Json, _: Context[IO]) => for {
        currentInvocations <- invocationQuota.get
        resp <- if (currentInvocations < 1) Json.obj().pure[IO] else IO.raiseError(new Exception("First invocation error"))
      } yield resp
      runtimeFiber <- LambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      secondInvocationResponse <- eventualResponse.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield {
      assert(invocationError eqv expectedErrorBody("First invocation error"))
      assert(secondInvocationResponse eqv "testId")
    }
  }

}
