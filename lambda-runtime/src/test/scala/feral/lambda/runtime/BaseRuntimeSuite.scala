package feral.lambda.runtime

import munit.CatsEffectSuite
import cats.effect.IO
import feral.lambda.{ClientContext, ClientContextClient, ClientContextEnv, CognitoIdentity, Context}
import io.circe.{Decoder, Encoder, Json, JsonObject, Printer}
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

abstract class BaseRuntimeSuite extends CatsEffectSuite {

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
        currentInvocations <- invocationQuota.modify(cur => (cur - 1, cur))
        _ <- if (currentInvocations > 0) IO.unit else IO.never
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
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "response" => Ok()
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "error" => Ok()
  }

  def testInvocationErrorRoute(eventualInvocationError: Deferred[IO, Json]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / _ / "error" =>
      for {
        body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
        _ <- eventualInvocationError.complete(body)
        resp <- Ok()
      } yield resp
  }

  def testNextInvocationRoute(eventualInvocationId: Deferred[IO, String]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "testApi" / FeralLambdaRuntime.ApiVersion / "runtime" / "invocation" / id / "response" =>
      eventualInvocationId.complete(id) >> Ok()
  }

  def invocationErrorRequest(errorMessage: String = "oops", errorType: String = "exception"): Json = Json.obj(
    "errorMessage" -> errorMessage.asJson,
    "errorType" -> errorType.asJson,
    "stackTrace" -> List[String]().asJson
  )

}
