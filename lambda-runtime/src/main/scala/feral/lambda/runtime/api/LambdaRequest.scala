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
package runtime.api
import cats.effect.Concurrent
import io.circe.Json
import org.http4s.EntityDecoder
import org.http4s.Response
import org.http4s.circe.jsonDecoderIncremental

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

import headers._

private[runtime] final class LambdaRequest(
    val deadlineTime: FiniteDuration,
    val id: String,
    val invokedFunctionArn: String,
    val identity: Option[CognitoIdentity],
    val clientContext: Option[ClientContext],
    val traceId: String,
    val body: Json
)
import cats.syntax.all._
private[runtime] object LambdaRequest {
  def fromResponse[F[_]](response: Response[F])(implicit F: Concurrent[F]): F[LambdaRequest] = {
    implicit val jsonDecoder: EntityDecoder[F, Json] = jsonDecoderIncremental
    for {
      _ <- F.raiseWhen(response.status.code === 500)(ContainerError)
      headers <-
        headersFrom(response).liftTo[F]
      (id, invokedFunctionArn, deadlineTimeInMs, traceId, cognitoIdentity, clientContext) =
        headers
      body <- response.as[Json]
    } yield {
      new LambdaRequest(
        FiniteDuration(deadlineTimeInMs.value, TimeUnit.MILLISECONDS),
        id.value,
        invokedFunctionArn.value,
        cognitoIdentity.map(_.value),
        clientContext.map(_.value),
        traceId.value,
        body
      )
    }
  }
  private def headersFrom[F[_]](response: Response[F]) = (
    response
      .headers
      .get[`Lambda-Runtime-Aws-Request-Id`]
      .toRightNec(`Lambda-Runtime-Aws-Request-Id`.name.toString),
    response
      .headers
      .get[`Lambda-Runtime-Invoked-Function-Arn`]
      .toRightNec(`Lambda-Runtime-Invoked-Function-Arn`.name.toString),
    response
      .headers
      .get[`Lambda-Runtime-Deadline-Ms`]
      .toRightNec(`Lambda-Runtime-Deadline-Ms`.name.toString),
    response
      .headers
      .get[`Lambda-Runtime-Trace-Id`]
      .toRightNec(`Lambda-Runtime-Trace-Id`.name.toString),
    response.headers.get[`Lambda-Runtime-Cognito-Identity`].rightNec,
    response.headers.get[`Lambda-Runtime-Client-Context`].rightNec)
    .parTupled
    .left
    .map(keys => new NoSuchElementException(s"${keys.intercalate(", ")} not found in headers"))
}
