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

import cats.effect.kernel.Concurrent
import io.circe._
import cats.syntax.all._
import feral.lambda._
import org.http4s.circe._
import org.http4s._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import feral.lambda.runtime.headers._

private[runtime] final class LambdaRequest(
    val deadlineTime: FiniteDuration,
    val id: String,
    val invokedFunctionArn: String,
    val identity: Option[CognitoIdentity],
    val clientContext: Option[ClientContext],
    val body: Json
)

private[runtime] object LambdaRequest {
  def fromResponse[F[_]](response: Response[F])(implicit F: Concurrent[F]): F[LambdaRequest] = {
    implicit val jsonDecoder: EntityDecoder[F, Json] = jsonDecoderIncremental
    for {
      id <- response
        .headers
        .get[`Lambda-Runtime-Aws-Request-Id`]
        .liftTo(new NoSuchElementException(`Lambda-Runtime-Aws-Request-Id`.name))
      invokedFunctionArn <- response
        .headers
        .get[`Lambda-Runtime-Invoked-Function-Arn`]
        .liftTo(new NoSuchElementException(`Lambda-Runtime-Invoked-Function-Arn`.name))
      deadlineTimeInMs <- response
        .headers
        .get[`Lambda-Runtime-Deadline-Ms`]
        .liftTo(new NoSuchElementException(`Lambda-Runtime-Deadline-Ms`.name))
      identity <- response.headers.get[`Lambda-Runtime-Client-Identity`].pure
      clientContext <- response.headers.get[`Lambda-Runtime-Client-Context`].pure
      body <- response.as[Json]
    } yield {
      new LambdaRequest(
        FiniteDuration(deadlineTimeInMs.value, TimeUnit.MILLISECONDS),
        id.value,
        invokedFunctionArn.value,
        identity.map(_.value),
        clientContext.map(_.value),
        body
      )
    }
  }
}
