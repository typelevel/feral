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
package events

import io.circe.Decoder

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/codecommit.d.ts
// https://docs.aws.amazon.com/lambda/latest/dg/services-codecommit.html

sealed abstract class CodeCommitEvent {
  def records: List[CodeCommitTrigger]
}

object CodeCommitEvent {

  def apply(records: List[CodeCommitTrigger]): CodeCommitEvent =
    new Impl(records)

  implicit val decoder: Decoder[CodeCommitEvent] =
    Decoder.forProduct1("Records")(CodeCommitEvent.apply)

  private final case class Impl(records: List[CodeCommitTrigger]) extends CodeCommitEvent {
    override def productPrefix = "CodeCommitEvent"
  }
}

sealed abstract class CodeCommitTrigger {
  def awsRegion: String
  def codecommit: CodeCommitReferences
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

object CodeCommitTrigger {

  def apply(
      awsRegion: String,
      codecommit: CodeCommitReferences,
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
  ): CodeCommitTrigger =
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

  private[events] implicit val decoder: Decoder[CodeCommitTrigger] =
    Decoder.instance(c =>
      for {
        awsRegion <- c.get[String]("awsRegion")
        codecommit <- c.get[CodeCommitReferences]("codecommit")
        customData <- c.get[Option[String]]("customData")
        eventId <- c.get[String]("eventId")
        eventName <- c.get[String]("eventName")
        eventPartNumber <- c.get[Int]("eventPartNumber")
        eventSource <- c.get[String]("eventSource")
        eventSourceArn <- c.get[String]("eventSourceARN")
        eventTime <- c.get[String]("eventTime")
        eventTotalParts <- c.get[Int]("eventTotalParts")
        eventTriggerConfigId <- c.get[String]("eventTriggerConfigId")
        eventTriggerName <- c.get[String]("eventTriggerName")
        eventVersion <- c.get[String]("eventVersion")
        userIdentityArn <- c.get[String]("userIdentityARN")
      } yield CodeCommitTrigger(
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
      ))

  private final case class Impl(
      awsRegion: String,
      codecommit: CodeCommitReferences,
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
  ) extends CodeCommitTrigger {
    override def productPrefix = "CodeCommitTrigger"
  }
}

sealed abstract class CodeCommitReferences {
  def references: List[CodeCommitReference]
}

object CodeCommitReferences {

  def apply(references: List[CodeCommitReference]): CodeCommitReferences =
    new Impl(references)

  private[events] implicit val decoder: Decoder[CodeCommitReferences] =
    Decoder.forProduct1("references")(CodeCommitReferences.apply)

  private final case class Impl(references: List[CodeCommitReference])
      extends CodeCommitReferences {
    override def productPrefix = "CodeCommitReferences"
  }
}

sealed abstract class CodeCommitReference {
  def commit: String
  def created: Option[Boolean]
  def deleted: Option[Boolean]
  def ref: String
}

object CodeCommitReference {

  def apply(
      commit: String,
      created: Option[Boolean],
      deleted: Option[Boolean],
      ref: String
  ): CodeCommitReference =
    new Impl(commit, created, deleted, ref)

  private[events] implicit val decoder: Decoder[CodeCommitReference] =
    Decoder.forProduct4("commit", "created", "deleted", "ref")(CodeCommitReference.apply)

  private final case class Impl(
      commit: String,
      created: Option[Boolean],
      deleted: Option[Boolean],
      ref: String
  ) extends CodeCommitReference {
    override def productPrefix = "CodeCommitReference"
  }
}
