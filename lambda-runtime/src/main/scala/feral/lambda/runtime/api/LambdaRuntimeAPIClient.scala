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

package feral.lambda.runtime.api
import cats.effect.Concurrent
import cats.syntax.all._
import feral.lambda.runtime.LambdaRuntimeEnv
import io.circe.Encoder
import org.http4s.EntityEncoder
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe.jsonEncoderOf
import org.http4s.client.Client

/**
 * AWS Lambda Runtime API Client
 */
private[runtime] trait LambdaRuntimeAPIClient[F[_]] {

  /**
   * Non-recoverable initialization error. Runtime should exit after reporting the error. Error
   * will be served in response to the first invoke.
   */
  def reportInitError(t: Throwable): F[Unit]

  /**
   * Runtime makes this request when it is ready to receive and process a new invoke.
   */
  def nextInvocation(): F[LambdaRequest]

  /**
   * Runtime makes this request in order to submit a response.
   */
  def submit[T: Encoder](awsRequestId: String, responseBody: T): F[Unit]

  /**
   * Runtime makes this request in order to submit an error response. It can be either a
   * function error, or a runtime error. Error will be served in response to the invoke.
   */
  def reportInvocationError(awsRequestId: String, t: Throwable): F[Unit]
}

private[runtime] object LambdaRuntimeAPIClient {
  final val ApiVersion = "2018-06-01"

  def apply[F[_]: Concurrent: LambdaRuntimeEnv](
      client: Client[F]): F[LambdaRuntimeAPIClient[F]] =
    LambdaRuntimeEnv[F].lambdaRuntimeApi.map { host =>
      val runtimeApi = host / ApiVersion / "runtime"
      new LambdaRuntimeAPIClient[F] {
        def reportInitError(t: Throwable): F[Unit] = client
          .run(
            Request[F]()
              .withMethod(Method.POST)
              .withUri(runtimeApi / "init" / "error")
              .withEntity(ErrorRequest.fromThrowable(t)))
          .use[Unit](handleResponse)

        def nextInvocation(): F[LambdaRequest] =
          client.get(runtimeApi / "invocation" / "next")(LambdaRequest.fromResponse[F])

        def submit[T: Encoder](awsRequestId: String, responseBody: T): F[Unit] = {
          implicit val enc: EntityEncoder[F, T] = jsonEncoderOf
          client
            .run(
              Request[F]()
                .withMethod(Method.POST)
                .withUri(runtimeApi / "invocation" / awsRequestId / "response")
                .withEntity(responseBody))
            .use[Unit](handleResponse)
        }

        def reportInvocationError(awsRequestId: String, t: Throwable): F[Unit] =
          client
            .run(
              Request[F]()
                .withMethod(Method.POST)
                .withUri(runtimeApi / "invocation" / awsRequestId / "error")
                .withEntity(ErrorRequest.fromThrowable(t))
            )
            .use[Unit](handleResponse)
      }
    }
  private def handleResponse[F[_]: Concurrent](response: Response[F]): F[Unit] =
    response.status match {
      case Status.InternalServerError => ContainerError.raiseError
      case Status.Accepted => ().pure
      case _ => response.as[ErrorResponse].flatMap(_.raiseError)
    }
}
