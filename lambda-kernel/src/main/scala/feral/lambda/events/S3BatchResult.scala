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

package feral.lambda.events

import io.circe.Encoder

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/s3-batch.d.ts

final case class S3BatchResult(
    invocationSchemaVersion: String,
    treatMissingKeysAs: S3BatchResultResultCode,
    invocationId: String,
    results: List[S3BatchResultResult]
)

object S3BatchResult {
  implicit val encoder: Encoder[S3BatchResult] =
    Encoder.forProduct4(
      "invocationSchemaVersion",
      "treatMissingKeysAs",
      "invocationId",
      "results")(r =>
      (r.invocationSchemaVersion, r.treatMissingKeysAs, r.invocationId, r.results))
}

sealed abstract class S3BatchResultResultCode

object S3BatchResultResultCode {
  case object Succeeded extends S3BatchResultResultCode
  case object TemporaryFailure extends S3BatchResultResultCode
  case object PermanentFailure extends S3BatchResultResultCode

  implicit val encoder: Encoder[S3BatchResultResultCode] = Encoder.encodeString.contramap {
    case Succeeded => "Succeeded"
    case TemporaryFailure => "TemporaryFailure"
    case PermanentFailure => "PermanentFailure"
  }
}

final case class S3BatchResultResult(
    taskId: String,
    resultCode: S3BatchResultResultCode,
    resultString: String)

object S3BatchResultResult {
  implicit val encoder: Encoder[S3BatchResultResult] =
    Encoder.forProduct3("taskId", "resultCode", "resultString")(r =>
      (r.taskId, r.resultCode, r.resultString))
}
