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

import feral.lambda.KernelSource
import io.circe.Decoder

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/s3-batch.d.ts

final case class S3BatchEvent(
    invocationSchemaVersion: String,
    invocationId: String,
    job: S3BatchJob,
    tasks: List[S3BatchEventTask]
)

object S3BatchEvent {
  implicit val decoder: Decoder[S3BatchEvent] =
    Decoder.forProduct4("invocationSchemaVersion", "invocationId", "job", "tasks")(
      S3BatchEvent.apply)

  implicit def kernelSource: KernelSource[S3BatchEvent] = KernelSource.emptyKernelSource
}

final case class S3BatchJob(id: String)

object S3BatchJob {
  implicit val decoder: Decoder[S3BatchJob] = Decoder.forProduct1("id")(S3BatchJob.apply)
}

final case class S3BatchEventTask(
    taskId: String,
    s3Key: String,
    s3VersionId: Option[String],
    s3BucketArn: String)

object S3BatchEventTask {
  implicit val decoder: Decoder[S3BatchEventTask] =
    Decoder.forProduct4("taskId", "s3Key", "s3VersionId", "s3BucketArn")(S3BatchEventTask.apply)
}
