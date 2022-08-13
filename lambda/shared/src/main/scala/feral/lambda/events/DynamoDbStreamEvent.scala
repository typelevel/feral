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
import io.circe.Json

final case class AttributeValue(
    b: Option[String],
    bs: Option[String],
    bool: Option[Boolean],
    l: Option[List[AttributeValue]],
    m: Option[Map[String, AttributeValue]],
    n: Option[String],
    ns: Option[List[String]],
    nul: Boolean,
    s: Option[String],
    ss: Option[List[String]]
)

object AttributeValue {
  implicit val decoder: Decoder[AttributeValue] = for {
    b <- Decoder[Option[String]].at("B")
    bs <- Decoder[Option[String]].at("BS")
    bool <- Decoder[Option[Boolean]].at("BOOL")
    l <- Decoder[Option[List[AttributeValue]]].at("L")
    m <- Decoder[Option[Map[String, AttributeValue]]].at("M")
    n <- Decoder[Option[String]].at("N")
    ns <- Decoder[Option[List[String]]].at("NS")
    nul <- Decoder[Option[true]].at("NULL")
    s <- Decoder[Option[String]].at("S")
    ss <- Decoder[Option[List[String]]].at("SS")
  } yield AttributeValue(
    b = b,
    bs = bs,
    bool = bool,
    l = l,
    m = m,
    n = n,
    ns = ns,
    nul = nul.getOrElse(false),
    s = s,
    ss = ss
  )
}

final case class StreamRecord(
    approximateCreationDateTime: Option[Double],
    keys: Option[Map[String, AttributeValue]],
    newImage: Option[Map[String, AttributeValue]],
    oldImage: Option[Map[String, AttributeValue]],
    sequenceNumber: Option[String],
    sizeBytes: Option[Double],
    streamViewType: Option[String]
)

object StreamRecord {
  implicit val decoder: Decoder[StreamRecord] = Decoder.forProduct7(
    "ApproximateCreationDateTime",
    "Keys",
    "NewImage",
    "OldImage",
    "SequenceNumber",
    "SizeBytes",
    "StreamViewType"
  )(StreamRecord.apply)
}

sealed abstract case class DynamoDbRecord private (
    awsRegion: Option[String],
    dynamodb: Option[StreamRecord],
    eventID: Option[String],
    eventName: Option[String],
    eventSource: Option[String],
    eventSourceArn: Option[String],
    eventVersion: Option[String],
    userIdentity: Option[Json]
)

object DynamoDbRecord {
  private[lambda] def apply(
      awsRegion: Option[String],
      dynamodb: Option[StreamRecord],
      eventID: Option[String],
      eventName: Option[String],
      eventSource: Option[String],
      eventSourceArn: Option[String],
      eventVersion: Option[String],
      userIdentity: Option[Json]
  ): DynamoDbRecord =
    new DynamoDbRecord(
      awsRegion,
      dynamodb,
      eventID,
      eventName,
      eventSource,
      eventSourceArn,
      eventVersion,
      userIdentity
    ) {}

  implicit val decoder: Decoder[DynamoDbRecord] = Decoder.forProduct8(
    "awsRegion",
    "dynamodb",
    "eventID",
    "eventName",
    "eventSource",
    "eventSourceARN",
    "eventVersion",
    "userIdentity"
  )(DynamoDbRecord.apply)
}

sealed abstract case class DynamoDbStreamEvent private (
    records: List[DynamoDbRecord]
)

object DynamoDbStreamEvent {
  private[lambda] def apply(records: List[DynamoDbRecord]): DynamoDbStreamEvent =
    new DynamoDbStreamEvent(records) {}

  implicit val decoder: Decoder[DynamoDbStreamEvent] =
    Decoder.forProduct1("Records")(DynamoDbStreamEvent.apply)

  implicit def kernelSource: KernelSource[DynamoDbStreamEvent] = KernelSource.emptyKernelSource
}
