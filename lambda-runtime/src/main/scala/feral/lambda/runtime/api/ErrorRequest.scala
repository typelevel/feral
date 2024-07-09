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

import io.circe._
import org.http4s.EntityEncoder
import org.http4s.circe._

private[runtime] final class ErrorRequest(
    val errorMessage: String,
    val errorType: String,
    val stackTrace: List[String])

private[runtime] object ErrorRequest {

  def apply(errorMessage: String, errorType: String, stackTrace: List[String]) =
    new ErrorRequest(errorMessage, errorType, stackTrace)

  def fromThrowable(ex: Throwable): ErrorRequest =
    ErrorRequest(
      ex.getMessage,
      ex.getClass.getSimpleName,
      ex.getStackTrace().toList.map(_.toString)
    )
  implicit val enc: Encoder[ErrorRequest] = Encoder.forProduct3(
    "errorMessage",
    "errorType",
    "stackTrace"
  )(ent => (ent.errorMessage, ent.errorType, ent.stackTrace))
  implicit def entEnc[F[_]]: EntityEncoder[F, ErrorRequest] =
    jsonEncoderOf[F, ErrorRequest]
}
