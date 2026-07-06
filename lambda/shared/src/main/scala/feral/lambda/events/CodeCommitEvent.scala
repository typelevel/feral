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

import io.circe.Decoder

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/codecommit.d.ts

sealed abstract class CodeCommitEvent {
  def records: List[CodeCommitRecord]
}

object CodeCommitEvent {
  def apply(records: List[CodeCommitRecord]): CodeCommitEvent =
    new Impl(records)

  implicit val decoder: Decoder[CodeCommitEvent] =
    Decoder.forProduct1("Records")(CodeCommitEvent.apply)

  private final case class Impl(
      records: List[CodeCommitRecord]
  ) extends CodeCommitEvent {
    override def productPrefix = "CodeCommitEvent"
  }
}

sealed abstract class CodeCommitRecord {
  def awsRegion: String
  def codecommit: CodeCommitData
  def customData: Option[String]
  def eventId: String
  def eventName: String
  def eventPartNumber: Int
  def eventSource: String
  def eventSourceArn: String
  def eventTime: String
  def eventTotalParts: Int
  def eventTriggerConfigId: String
  def eventTriggerName: String
  def eventVersion: String
  def userIdentityArn: String
}

object CodeCommitRecord {
  def apply(
      awsRegion: String,
      codecommit: CodeCommitData,
      customData: Option[String],
      eventId: String,
      eventName: String,
      eventPartNumber: Int,
      eventSource: String,
      eventSourceArn: String,
      eventTime: String,
      eventTotalParts: Int,
      eventTriggerConfigId: String,
      eventTriggerName: String,
      eventVersion: String,
      userIdentityArn: String
  ): CodeCommitRecord =
    new Impl(
      awsRegion,
      codecommit,
      customData,
      eventId,
      eventName,
      eventPartNumber,
      eventSource,
      eventSourceArn,
      eventTime,
      eventTotalParts,
      eventTriggerConfigId,
      eventTriggerName,
      eventVersion,
      userIdentityArn
    )

  private[events] implicit val decoder: Decoder[CodeCommitRecord] =
    Decoder.forProduct14(
      "awsRegion",
      "codecommit",
      "customData",
      "eventId",
      "eventName",
      "eventPartNumber",
      "eventSource",
      "eventSourceARN",
      "eventTime",
      "eventTotalParts",
      "eventTriggerConfigId",
      "eventTriggerName",
      "eventVersion",
      "userIdentityARN"
    )(CodeCommitRecord.apply)

  private final case class Impl(
      awsRegion: String,
      codecommit: CodeCommitData,
      customData: Option[String],
      eventId: String,
      eventName: String,
      eventPartNumber: Int,
      eventSource: String,
      eventSourceArn: String,
      eventTime: String,
      eventTotalParts: Int,
      eventTriggerConfigId: String,
      eventTriggerName: String,
      eventVersion: String,
      userIdentityArn: String
  ) extends CodeCommitRecord {
    override def productPrefix = "CodeCommitRecord"
  }
}

sealed abstract class CodeCommitData {
  def references: List[CodeCommitReference]
}

object CodeCommitData {
  def apply(references: List[CodeCommitReference]): CodeCommitData =
    new Impl(references)

  private[events] implicit val decoder: Decoder[CodeCommitData] =
    Decoder.forProduct1("references")(CodeCommitData.apply)

  private final case class Impl(
      references: List[CodeCommitReference]
  ) extends CodeCommitData {
    override def productPrefix = "CodeCommitData"
  }
}

sealed abstract class CodeCommitReference {
  def commit: String
  def ref: String
  def created: Option[Boolean]
  def deleted: Option[Boolean]
}

object CodeCommitReference {
  def apply(
      commit: String,
      ref: String,
      created: Option[Boolean],
      deleted: Option[Boolean]
  ): CodeCommitReference =
    new Impl(commit, ref, created, deleted)

  private[events] implicit val decoder: Decoder[CodeCommitReference] =
    Decoder.forProduct4(
      "commit",
      "ref",
      "created",
      "deleted"
    )(CodeCommitReference.apply)

  private final case class Impl(
      commit: String,
      ref: String,
      created: Option[Boolean],
      deleted: Option[Boolean]
  ) extends CodeCommitReference {
    override def productPrefix = "CodeCommitReference"
  }
}
