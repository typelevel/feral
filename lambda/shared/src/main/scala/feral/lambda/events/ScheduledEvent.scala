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
import io.circe.JsonObject

import java.time.Instant

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/eventbridge.d.ts
// https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-events/src/main/java/com/amazonaws/services/lambda/runtime/events/ScheduledEvent.java
sealed abstract class ScheduledEvent {
  def id: String
  def version: Option[String]
  def account: String
  def time: Instant
  def region: String
  def resources: List[String]
  def source: String
  def `detail-type`: String
  def detail: JsonObject
  def `replay-name`: Option[String]
}

object ScheduledEvent {

  def apply(
      id: String,
      version: Option[String],
      account: String,
      time: Instant,
      region: String,
      resources: List[String],
      source: String,
      `detail-type`: String,
      detail: JsonObject,
      `replay-name`: Option[String]
  ): ScheduledEvent =
    new Impl(
      id,
      version,
      account,
      time,
      region,
      resources,
      source,
      `detail-type`,
      detail,
      `replay-name`
    )

  implicit val decoder: Decoder[ScheduledEvent] = Decoder.forProduct10(
    "id",
    "version",
    "account",
    "time",
    "region",
    "resources",
    "source",
    "detail-type",
    "detail",
    "replay-name"
  )(ScheduledEvent.apply)

  private final case class Impl(
      id: String,
      version: Option[String],
      account: String,
      time: Instant,
      region: String,
      resources: List[String],
      source: String,
      `detail-type`: String,
      detail: JsonObject,
      `replay-name`: Option[String]
  ) extends ScheduledEvent {
    override def productPrefix = "ScheduledEvent"
  }
}
