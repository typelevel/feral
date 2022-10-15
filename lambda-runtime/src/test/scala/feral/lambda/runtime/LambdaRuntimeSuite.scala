package feral.lambda.runtime

import cats.effect.IO
import feral.lambda.{ClientContext, ClientContextClient, ClientContextEnv, CognitoIdentity, Context}
import io.circe.{Decoder, Json, JsonObject, Printer}
import cats.syntax.all._
import cats.effect._
import cats.effect.syntax.all._
import io.circe.syntax.EncoderOps
import munit._
import org.http4s.Method.{GET, POST}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.Uri.Path.Root
import org.http4s._
import org.http4s.client.Client
import org.typelevel.jawn.Parser
import io.circe.jawn.CirceSupportParser.facade
import io.circe.jawn.parse
import org.http4s.dsl.io._
import feral.lambda.runtime.headers._
import org.http4s.circe.jsonEncoderWithPrinter

import scala.concurrent.duration.DurationInt

class LambdaRuntimeSuite extends CatsEffectSuite {

  implicit val jsonEncoder: EntityEncoder[IO, Json] = jsonEncoderWithPrinter[IO](Printer.noSpaces.copy(dropNullValues = true))

  def createTestEnv(funcName: IO[String] = IO("test"),
                    memorySize: IO[Int] = IO(144),
                    funcVersion: IO[String] = IO("1.0"),
                    logGroupName: IO[String] = IO("test"),
                    logStreamName: IO[String] = IO("test"),
                    runtimeApi: IO[String] = IO("testApi")
                   ): LambdaRuntimeEnv[IO] = new LambdaRuntimeEnv[IO] {
    override def lambdaFunctionName: IO[String] = funcName

    override def lambdaFunctionMemorySize: IO[Int] = memorySize

    override def lambdaFunctionVersion: IO[String] = funcVersion

    override def lambdaLogGroupName: IO[String] = logGroupName

    override def lambdaLogStreamName: IO[String] = logStreamName

    override def lambdaRuntimeApi: IO[String] = runtimeApi
  }

  def defaultRoutes(invocationQuota: Ref[IO, Int]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / "next" =>
      for {
        currentInvocations <- invocationQuota.modify(cur => (cur-1, cur))
        _ <- if (currentInvocations > 0) IO.unit else IO.never // infinite loop if not first request, assumed behaviour of actual next invocation endpoint?
        headers = Headers(
          `Lambda-Runtime-Aws-Request-Id`("testId"),
          `Lambda-Runtime-Deadline-Ms`(20),
          `Lambda-Runtime-Invoked-Function-Arn`("test"),
          `Lambda-Runtime-Client-Context`(new ClientContext(
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
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" => Ok()
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" => Ok()
  }

  test("The runtime can process an event and pass the result to the invocation response url") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationId <- Deferred[IO, String]
      client = Client.fromHttpApp[IO]((HttpRoutes.of[IO] {
        case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" =>
          eventualInvocationId.complete(id) >> Ok()
      } <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => JsonObject.empty.asJson.pure[IO]
      runtimeFiber <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
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
      handler = (json: Json, context: Context[IO]) => eventualInvocation.complete((json, context)) >> JsonObject.empty.asJson.pure[IO]
      runtimeFiber <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      (jsonEvent, context) <- eventualInvocation.get.timeout(2.seconds)
      expectedJson = Json.obj("eventField" -> "test".asJson)
      _ <- runtimeFiber.cancel
    } yield {
      assert(jsonEvent eqv expectedJson)
      assert(context.clientContext.get.client.appTitle == "test")
      assert(context.identity.get.identityId == "test")
      assert(context.functionName == "test")
      assert(context.memoryLimitInMB == 144)
    }
  }

  test("The runtime will call the initialization error url when ???") {

  }

  test("The runtime will call the invocation error url when the handler function errors during processing") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((HttpRoutes.of[IO] {
        case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" =>
          for {
            body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
            _ <- eventualInvocationError.complete(body)
            resp <- Ok()
          } yield resp
      } <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => IO.raiseError[Json](new Exception("oops"))
      runtimeFiber <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      expectedBody = Json.obj(
        "errorMessage" -> "oops".asJson,
        "errorType" -> "exception".asJson,
        "stackTrace" -> List[String]().asJson
      )
      _ <- runtimeFiber.cancel
    } yield assert(invocationError eqv expectedBody)
  }

  test("The runtime will call the invocation error url when a needed environment variable is not available") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv(funcName = IO.raiseError(new Exception("oops")))
    for {
      invocationQuota <- Ref[IO].of(1)
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((HttpRoutes.of[IO] {
        case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" =>
          for {
            body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
            _ <- eventualInvocationError.complete(body)
            resp <- Ok()
          } yield resp
      } <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => JsonObject.empty.asJson.pure[IO]
      runtimeFiber <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      expectedBody = Json.obj(
        "errorMessage" -> "oops".asJson,
        "errorType" -> "exception".asJson,
        "stackTrace" -> List[String]().asJson
      )
      _ <- runtimeFiber.cancel
    } yield assert(invocationError eqv expectedBody)

  }

  // TODO needs refactoring
  test("The runtime will continue to process events even after encountering an invocation error") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Ref[IO].of(2)
      eventualInvocationError <- Deferred[IO, Json]
      eventualResponse <- Deferred[IO, String]
      client = Client.fromHttpApp((HttpRoutes.of[IO] {
        case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" =>
          for {
            body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
            _ <- eventualInvocationError.complete(body)
            resp <- Ok()
          } yield resp
        case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" =>
          eventualResponse.complete(id) >> Ok()
      } <+> defaultRoutes(invocationQuota)).orNotFound)
      handler = (_: Json, _: Context[IO]) => for {
        currentInvocations <- invocationQuota.get
        resp <- if (currentInvocations < 1) JsonObject.empty.asJson.pure[IO] else IO.raiseError(new Exception("oops first invocation"))
      } yield resp
      runtimeFiber <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO])).start
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      expectedErrorBody = Json.obj(
        "errorMessage" -> "oops first invocation".asJson,
        "errorType" -> "exception".asJson,
        "stackTrace" -> List[String]().asJson
      )
      secondInvocationResponse <- eventualResponse.get.timeout(2.seconds)
      _ <- runtimeFiber.cancel
    } yield {
      assert(invocationError eqv expectedErrorBody)
      assert(secondInvocationResponse eqv "testId")
    }
  }

}
