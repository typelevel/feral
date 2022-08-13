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

sealed abstract case class KinesisStreamRecordPayload private (
    approximateArrivalTimestamp: Instant,
    data: ByteVector,
    kinesisSchemaVersion: String,
    partitionKey: String,
    sequenceNumber: String
)

object KinesisStreamRecordPayload {
  private[lambda] def apply(
      approximateArrivalTimestamp: Instant,
      data: ByteVector,
      kinesisSchemaVersion: String,
      partitionKey: String,
      sequenceNumber: String
  ): KinesisStreamRecordPayload =
    new KinesisStreamRecordPayload(
      approximateArrivalTimestamp,
      data,
      kinesisSchemaVersion,
      partitionKey,
      sequenceNumber
    ) {}

  implicit val decoder: Decoder[KinesisStreamRecordPayload] = Decoder.forProduct5(
    "approximateArrivalTimestamp",
    "data",
    "kinesisSchemaVersion",
    "partitionKey",
    "sequenceNumber"
  )(KinesisStreamRecordPayload.apply)
}

sealed abstract case class KinesisStreamRecord private (
    awsRegion: String,
    eventID: String,
    eventName: String,
    eventSource: String,
    eventSourceArn: String,
    eventVersion: String,
    invokeIdentityArn: String,
    kinesis: KinesisStreamRecordPayload
)

object KinesisStreamRecord {
  private[lambda] def apply(
      awsRegion: String,
      eventID: String,
      eventName: String,
      eventSource: String,
      eventSourceArn: String,
      eventVersion: String,
      invokeIdentityArn: String,
      kinesis: KinesisStreamRecordPayload
  ): KinesisStreamRecord =
    new KinesisStreamRecord(
      awsRegion,
      eventID,
      eventName,
      eventSource,
      eventSourceArn,
      eventVersion,
      invokeIdentityArn,
      kinesis
    ) {}

  implicit val decoder: Decoder[KinesisStreamRecord] = Decoder.forProduct8(
    "awsRegion",
    "eventID",
    "eventName",
    "eventSource",
    "eventSourceARN",
    "eventVersion",
    "invokeIdentityArn",
    "kinesis"
  )(KinesisStreamRecord.apply)
}

sealed abstract case class KinesisStreamEvent private (
    records: List[KinesisStreamRecord]
)

object KinesisStreamEvent {
  private[lambda] def apply(records: List[KinesisStreamRecord]): KinesisStreamEvent =
    new KinesisStreamEvent(records) {}

  implicit val decoder: Decoder[KinesisStreamEvent] =
    Decoder.forProduct1("Records")(KinesisStreamEvent.apply)

  implicit def kernelSource: KernelSource[KinesisStreamEvent] = KernelSource.emptyKernelSource
}
