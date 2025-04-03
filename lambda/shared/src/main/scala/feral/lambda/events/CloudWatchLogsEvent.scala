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

sealed abstract class CloudWatchLogsEvent {
  def awslogs: CloudWatchLogsEventData
}

object CloudWatchLogsEvent {
  def apply(awslogs: CloudWatchLogsEventData): CloudWatchLogsEvent = new Impl(
    awslogs
  )

  implicit val decoder: Decoder[CloudWatchLogsEvent] =
    Decoder.forProduct1("awslogs")(CloudWatchLogsEvent.apply)

  private final case class Impl(awslogs: CloudWatchLogsEventData)
      extends CloudWatchLogsEvent {
    override def productPrefix = "CloudWatchLogsEvent"
  }
}

sealed abstract class CloudWatchLogsEventData {
  def data: String
}

object CloudWatchLogsEventData {
  def apply(data: String): CloudWatchLogsEventData = new Impl(data)

  implicit val decoder: Decoder[CloudWatchLogsEventData] =
    Decoder.forProduct1("data")(CloudWatchLogsEventData.apply)

  private final case class Impl(data: String) extends CloudWatchLogsEventData {
    override def productPrefix = "CloudWatchLogsEventData"
  }
}

sealed abstract class CloudWatchLogsDecodedData {
  def owner: String
  def logGroup: String
  def logStream: String
  def subscriptionFilters: List[String]
  def messageType: String
  def logEvents: List[CloudWatchLogsLogEvent]
}

object CloudWatchLogsDecodedData {
  def apply(
      owner: String,
      logGroup: String,
      logStream: String,
      subscriptionFilters: List[String],
      messageType: String,
      logEvents: List[CloudWatchLogsLogEvent]
  ): CloudWatchLogsDecodedData =
    new Impl(
      owner,
      logGroup,
      logStream,
      subscriptionFilters,
      messageType,
      logEvents
    )

  implicit val decoder: Decoder[CloudWatchLogsDecodedData] =
    Decoder.forProduct6(
      "owner",
      "logGroup",
      "logStream",
      "subscriptionFilters",
      "messageType",
      "logEvents"
    )(CloudWatchLogsDecodedData.apply)

  private final case class Impl(
      owner: String,
      logGroup: String,
      logStream: String,
      subscriptionFilters: List[String],
      messageType: String,
      logEvents: List[CloudWatchLogsLogEvent]
  ) extends CloudWatchLogsDecodedData {
    override def productPrefix = "CloudWatchLogsDecodedData"
  }
}

sealed abstract class CloudWatchLogsLogEvent {
  def id: String
  def timestamp: Long
  def message: String
  def extractedFields: Option[Map[String, String]]
}

object CloudWatchLogsLogEvent {
  def apply(
      id: String,
      timestamp: Long,
      message: String,
      extractedFields: Option[Map[String, String]]
  ): CloudWatchLogsLogEvent =
    new Impl(id, timestamp, message, extractedFields)

  implicit val decoder: Decoder[CloudWatchLogsLogEvent] = Decoder.forProduct4(
    "id",
    "timestamp",
    "message",
    "extractedFields"
  )(CloudWatchLogsLogEvent.apply)

  private final case class Impl(
      id: String,
      timestamp: Long,
      message: String,
      extractedFields: Option[Map[String, String]]
  ) extends CloudWatchLogsLogEvent {
    override def productPrefix = "CloudWatchLogsLogEvent"
  }
}
