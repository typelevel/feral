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
import io.circe.scodec._
import scodec.bits.ByteVector

import java.time.Instant

sealed abstract class KinesisStreamRecordPayload {
  def approximateArrivalTimestamp: Instant
  def data: ByteVector
  def kinesisSchemaVersion: String
  def partitionKey: String
  def sequenceNumber: String
}

object KinesisStreamRecordPayload {
  def apply(
      approximateArrivalTimestamp: Instant,
      data: ByteVector,
      kinesisSchemaVersion: String,
      partitionKey: String,
      sequenceNumber: String
  ): KinesisStreamRecordPayload =
    new Impl(
      approximateArrivalTimestamp,
      data,
      kinesisSchemaVersion,
      partitionKey,
      sequenceNumber
    )

  import codecs.decodeInstant
  private[events] implicit val decoder: Decoder[KinesisStreamRecordPayload] =
    Decoder.forProduct5(
      "approximateArrivalTimestamp",
      "data",
      "kinesisSchemaVersion",
      "partitionKey",
      "sequenceNumber"
    )(KinesisStreamRecordPayload.apply)

  private final case class Impl(
      approximateArrivalTimestamp: Instant,
      data: ByteVector,
      kinesisSchemaVersion: String,
      partitionKey: String,
      sequenceNumber: String
  ) extends KinesisStreamRecordPayload {
    override def productPrefix = "KinesisStreamRecordPayload"
  }
}

sealed abstract class KinesisStreamRecord {
  def awsRegion: String
  def eventId: String
  def eventName: String
  def eventSource: String
  def eventSourceArn: String
  def eventVersion: String
  def invokeIdentityArn: String
  def kinesis: KinesisStreamRecordPayload
}

object KinesisStreamRecord {
  def apply(
      awsRegion: String,
      eventId: String,
      eventName: String,
      eventSource: String,
      eventSourceArn: String,
      eventVersion: String,
      invokeIdentityArn: String,
      kinesis: KinesisStreamRecordPayload
  ): KinesisStreamRecord =
    new Impl(
      awsRegion,
      eventId,
      eventName,
      eventSource,
      eventSourceArn,
      eventVersion,
      invokeIdentityArn,
      kinesis
    )

  private[events] implicit val decoder: Decoder[KinesisStreamRecord] = Decoder.forProduct8(
    "awsRegion",
    "eventID",
    "eventName",
    "eventSource",
    "eventSourceARN",
    "eventVersion",
    "invokeIdentityArn",
    "kinesis"
  )(KinesisStreamRecord.apply)

  private final case class Impl(
      awsRegion: String,
      eventId: String,
      eventName: String,
      eventSource: String,
      eventSourceArn: String,
      eventVersion: String,
      invokeIdentityArn: String,
      kinesis: KinesisStreamRecordPayload
  ) extends KinesisStreamRecord {
    override def productPrefix = "KinesisStreamRecord"
  }
}

sealed abstract class KinesisStreamEvent {
  def records: List[KinesisStreamRecord]
}

object KinesisStreamEvent {
  def apply(records: List[KinesisStreamRecord]): KinesisStreamEvent =
    new Impl(records)

  implicit val decoder: Decoder[KinesisStreamEvent] =
    Decoder.forProduct1("Records")(KinesisStreamEvent.apply)

  implicit def kernelSource: KernelSource[KinesisStreamEvent] = KernelSource.emptyKernelSource

  private final case class Impl(
      records: List[KinesisStreamRecord]
  ) extends KinesisStreamEvent {
    override def productPrefix = "KinesisStreamEvent"
  }
}
