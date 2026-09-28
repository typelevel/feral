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
import io.circe.scodec.decodeByteVector
import scodec.bits.ByteVector

sealed abstract class AttributeValue {
  def b: Option[ByteVector]
  def bs: Option[List[ByteVector]]
  def bool: Option[Boolean]
  def l: Option[List[AttributeValue]]
  def m: Option[Map[String, AttributeValue]]
  def n: Option[String]
  def ns: Option[List[String]]
  def nul: Boolean
  def s: Option[String]
  def ss: Option[List[String]]
}

object AttributeValue {
  def apply(
      b: Option[ByteVector],
      bs: Option[List[ByteVector]],
      bool: Option[Boolean],
      l: Option[List[AttributeValue]],
      m: Option[Map[String, AttributeValue]],
      n: Option[String],
      ns: Option[List[String]],
      nul: Boolean,
      s: Option[String],
      ss: Option[List[String]]
  ): AttributeValue =
    new Impl(b, bs, bool, l, m, n, ns, nul, s, ss)

  private[events] implicit val decoder: Decoder[AttributeValue] = for {
    b <- Decoder[Option[ByteVector]].at("B")
    bs <- Decoder[Option[List[ByteVector]]].at("BS")
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

  private final case class Impl(
      b: Option[ByteVector],
      bs: Option[List[ByteVector]],
      bool: Option[Boolean],
      l: Option[List[AttributeValue]],
      m: Option[Map[String, AttributeValue]],
      n: Option[String],
      ns: Option[List[String]],
      nul: Boolean,
      s: Option[String],
      ss: Option[List[String]]
  ) extends AttributeValue {
    override def productPrefix = "AttributeValue"
  }
}

sealed abstract class StreamRecord {
  def approximateCreationDateTime: Option[Double]
  def keys: Option[Map[String, AttributeValue]]
  def newImage: Option[Map[String, AttributeValue]]
  def oldImage: Option[Map[String, AttributeValue]]
  def sequenceNumber: Option[String]
  def sizeBytes: Option[Double]
  def streamViewType: Option[String]
}

object StreamRecord {
  def apply(
      approximateCreationDateTime: Option[Double],
      keys: Option[Map[String, AttributeValue]],
      newImage: Option[Map[String, AttributeValue]],
      oldImage: Option[Map[String, AttributeValue]],
      sequenceNumber: Option[String],
      sizeBytes: Option[Double],
      streamViewType: Option[String]
  ): StreamRecord =
    new Impl(
      approximateCreationDateTime,
      keys,
      newImage,
      oldImage,
      sequenceNumber,
      sizeBytes,
      streamViewType
    )

  private[events] implicit val decoder: Decoder[StreamRecord] = Decoder.forProduct7(
    "ApproximateCreationDateTime",
    "Keys",
    "NewImage",
    "OldImage",
    "SequenceNumber",
    "SizeBytes",
    "StreamViewType"
  )(StreamRecord.apply)

  private final case class Impl(
      approximateCreationDateTime: Option[Double],
      keys: Option[Map[String, AttributeValue]],
      newImage: Option[Map[String, AttributeValue]],
      oldImage: Option[Map[String, AttributeValue]],
      sequenceNumber: Option[String],
      sizeBytes: Option[Double],
      streamViewType: Option[String]
  ) extends StreamRecord {
    override def productPrefix = "StreamRecord"
  }
}

sealed abstract class DynamoDbRecord {
  def awsRegion: Option[String]
  def dynamodb: Option[StreamRecord]
  def eventId: Option[String]
  def eventName: Option[String]
  def eventSource: Option[String]
  def eventSourceArn: Option[String]
  def eventVersion: Option[String]
  def userIdentity: Option[Json]

  @deprecated("Renamed to eventId", "0.3.0")
  final def eventID: Option[String] = eventId
}

object DynamoDbRecord {
  def apply(
      awsRegion: Option[String],
      dynamodb: Option[StreamRecord],
      eventId: Option[String],
      eventName: Option[String],
      eventSource: Option[String],
      eventSourceArn: Option[String],
      eventVersion: Option[String],
      userIdentity: Option[Json]
  ): DynamoDbRecord =
    new Impl(
      awsRegion,
      dynamodb,
      eventId,
      eventName,
      eventSource,
      eventSourceArn,
      eventVersion,
      userIdentity
    )

  private[events] implicit val decoder: Decoder[DynamoDbRecord] = Decoder.forProduct8(
    "awsRegion",
    "dynamodb",
    "eventID",
    "eventName",
    "eventSource",
    "eventSourceARN",
    "eventVersion",
    "userIdentity"
  )(DynamoDbRecord.apply)

  private final case class Impl(
      awsRegion: Option[String],
      dynamodb: Option[StreamRecord],
      eventId: Option[String],
      eventName: Option[String],
      eventSource: Option[String],
      eventSourceArn: Option[String],
      eventVersion: Option[String],
      userIdentity: Option[Json]
  ) extends DynamoDbRecord {
    override def productPrefix = "DynamoDbRecord"
  }
}

sealed abstract class DynamoDbStreamEvent {
  def records: List[DynamoDbRecord]
}

object DynamoDbStreamEvent {
  def apply(records: List[DynamoDbRecord]): DynamoDbStreamEvent =
    new Impl(records)

  implicit val decoder: Decoder[DynamoDbStreamEvent] =
    Decoder.forProduct1("Records")(DynamoDbStreamEvent.apply)

  private final case class Impl(
      records: List[DynamoDbRecord]
  ) extends DynamoDbStreamEvent {
    override def productPrefix = "DynamoDbStreamEvent"
  }
}
