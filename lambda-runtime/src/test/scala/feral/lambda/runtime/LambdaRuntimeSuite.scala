package feral.lambda.runtime

import cats.effect.IO
import feral.lambda.{ClientContext, ClientContextClient, ClientContextEnv, CognitoIdentity, Context}
import io.circe.{Json, JsonObject, Printer}
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

  def defaultRoutes(invocationQuota: Deferred[IO, Unit]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / "next" =>
      (for {
        firstInvoc <- invocationQuota.complete() // used so response is only returned on first request to prevent infinite loop of runtime
        _ <- IO.raiseWhen(!firstInvoc)(new Exception("not first invocation"))
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
      } yield resp).handleErrorWith(_ => NotFound("sdfsdf")) // work in progress, need to figure out how lambda knows how to stop running
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" => Ok()
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" => Ok()
  }

  test("The runtime can process an event and pass the result to the invocation response url") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Deferred[IO, Unit]
      eventualInvocationId <- Deferred[IO, String]
      client = Client.fromHttpApp[IO]((defaultRoutes(invocationQuota) <+> HttpRoutes.of[IO] {
        case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" =>
          eventualInvocationId.complete(id) >> Ok()
      }).orNotFound)
      handler = (_: Json, _: Context[IO]) => JsonObject.empty.asJson.pure[IO]
      _ <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO]))
      invocationId <- eventualInvocationId.get.timeout(2.seconds)
    } yield assert(invocationId == "testId")
  }

  test("A valid context and JSON event is passed to the handler function during invocation") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv()
    for {
      invocationQuota <- Deferred[IO, Unit]
      eventualInvocation <- Deferred[IO, (Json, Context[IO])]
      client = Client.fromHttpApp[IO](defaultRoutes(invocationQuota).orNotFound)
      handler = (json: Json, context: Context[IO]) => eventualInvocation.complete((json, context)) >> JsonObject.empty.asJson.pure[IO]
      _ <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO]))
      (jsonEvent, context) <- eventualInvocation.get.timeout(2.seconds)
      expectedJson = Json.obj("eventField" -> "test".asJson)
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
      invocationQuota <- Deferred[IO, Unit]
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((defaultRoutes(invocationQuota) <+> HttpRoutes.of[IO] {
        case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" =>
          for {
            body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
            _ <- eventualInvocationError.complete(body)
            resp <- Ok()
          } yield resp
      }).orNotFound)
      handler = (_: Json, _: Context[IO]) => IO.raiseError[Json](new Exception("oops"))
      _ <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO]))
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      expectedBody = Json.obj(
        "errorMessage" -> "error".asJson,
        "errorType" -> "error".asJson,
        "stackTrace" -> List[String]().asJson
      )
    } yield assert(invocationError eqv expectedBody)
  }

  test("The runtime will call the invocation error url when a needed environment variable is not available") {
    implicit val env: LambdaRuntimeEnv[IO] = createTestEnv(funcName = IO.raiseError(new Exception("oops")))
    for {
      invocationQuota <- Deferred[IO, Unit]
      eventualInvocationError <- Deferred[IO, Json]
      client = Client.fromHttpApp((defaultRoutes(invocationQuota) <+> HttpRoutes.of[IO] {
        case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "error" =>
          for {
            body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
            _ <- eventualInvocationError.complete(body)
            resp <- Ok()
          } yield resp
      }).orNotFound)
      handler = (_: Json, _: Context[IO]) => JsonObject.empty.asJson.pure[IO]
      _ <- FeralLambdaRuntime(client)(Resource.eval(handler.pure[IO]))
      invocationError <- eventualInvocationError.get.timeout(2.seconds)
      expectedBody = Json.obj(
        "errorMessage" -> "error".asJson,
        "errorType" -> "error".asJson,
        "stackTrace" -> List[String]().asJson
      )
    } yield assert(invocationError eqv expectedBody)

  }

}
