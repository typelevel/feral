package feral.lambda.events

import java.time.Instant

import io.circe.{Decoder, JsonObject}

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/eventbridge.d.ts
// https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-events/src/main/java/com/amazonaws/services/lambda/runtime/events/ScheduledEvent.java
sealed abstract class ScheduledEvent {
  def id: String
  def version: String
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
      version: String,
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
      version: String,
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
