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

sealed abstract class S3BatchEvent {
  def invocationSchemaVersion: String
  def invocationId: String
  def job: S3BatchEventJob
  def tasks: List[S3BatchEventTask]
}

object S3BatchEvent {
  def apply(
      invocationSchemaVersion: String,
      invocationId: String,
      job: S3BatchEventJob,
      tasks: List[S3BatchEventTask]
  ): S3BatchEvent =
    new Impl(invocationSchemaVersion, invocationId, job, tasks)

  implicit val decoder: Decoder[S3BatchEvent] =
    Decoder.forProduct4("invocationSchemaVersion", "invocationId", "job", "tasks")(
      S3BatchEvent.apply)

  implicit def kernelSource: KernelSource[S3BatchEvent] = KernelSource.emptyKernelSource

  private final case class Impl(
      invocationSchemaVersion: String,
      invocationId: String,
      job: S3BatchEventJob,
      tasks: List[S3BatchEventTask]
  ) extends S3BatchEvent {
    override def productPrefix = "S3BatchEvent"
  }
}

sealed abstract class S3BatchEventJob {
  def id: String
}

object S3BatchEventJob {
  def apply(id: String): S3BatchEventJob = new Impl(id)

  private[events] implicit val decoder: Decoder[S3BatchEventJob] =
    Decoder.forProduct1("id")(S3BatchEventJob.apply)

  private final case class Impl(id: String) extends S3BatchEventJob {
    override def productPrefix = "S3BatchEventJob"
  }
}

sealed abstract class S3BatchEventTask {
  def taskId: String
  def s3Key: String
  def s3VersionId: Option[String]
  def s3BucketArn: String
}

object S3BatchEventTask {
  def apply(
      taskId: String,
      s3Key: String,
      s3VersionId: Option[String],
      s3BucketArn: String
  ): S3BatchEventTask =
    new Impl(taskId, s3Key, s3VersionId, s3BucketArn)

  private[events] implicit val decoder: Decoder[S3BatchEventTask] =
    Decoder.forProduct4("taskId", "s3Key", "s3VersionId", "s3BucketArn")(S3BatchEventTask.apply)

  private final case class Impl(
      taskId: String,
      s3Key: String,
      s3VersionId: Option[String],
      s3BucketArn: String)
      extends S3BatchEventTask {
    override def productPrefix = "S3BatchEventTask"
  }
}
