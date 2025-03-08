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

import cats.implicits._
import com.comcast.ip4s.Host
import com.comcast.ip4s.SocketAddress
import io.circe.Decoder
import io.circe.KeyDecoder
import scodec.bits.ByteVector

import java.time.Instant

sealed abstract class MSKEvent extends KafkaEvent {
  def eventSourceArn: String
}

object MSKEvent {
  def apply(
      records: Map[TopicPartition, List[KafkaRecord]],
      eventSource: String,
      eventSourceArn: String,
      bootstrapServers: List[SocketAddress[Host]]
  ): MSKEvent = {
    Impl(records, eventSource, eventSourceArn, bootstrapServers)
  }

  import feral.lambda.events.KafkaEvent.bootstrapServersDecoder
  private[events] implicit val decoder: Decoder[MSKEvent] =
    Decoder.forProduct4(
      "records",
      "eventSource",
      "eventSourceArn",
      "bootstrapServers"
    )(MSKEvent.apply)

  private final case class Impl(
      records: Map[TopicPartition, List[KafkaRecord]],
      eventSource: String,
      eventSourceArn: String,
      bootstrapServers: List[SocketAddress[Host]]
  ) extends MSKEvent {
    override def productPrefix = "KafkaEvent"
  }
}

sealed abstract class KafkaEvent {
  def records: Map[TopicPartition, List[KafkaRecord]]
  def eventSource: String
  def bootstrapServers: List[SocketAddress[Host]]
}

object KafkaEvent {
  def apply(
      records: Map[TopicPartition, List[KafkaRecord]],
      eventSource: String,
      bootstrapServers: List[SocketAddress[Host]]
  ): KafkaEvent = {
    Impl(records, eventSource, bootstrapServers)
  }

  private[events] implicit val bootstrapServersDecoder: Decoder[List[SocketAddress[Host]]] =
    Decoder
      .decodeString
      .emap(str =>
        str
          .split(",")
          .toList
          .map(SocketAddress.fromString)
          .sequence
          .toRight(s"Failed to parse bootstrap servers: $str"))

  private[events] implicit val decoder: Decoder[KafkaEvent] =
    Decoder.forProduct3(
      "records",
      "eventSource",
      "bootstrapServers"
    )(KafkaEvent.apply)

  private final case class Impl(
      records: Map[TopicPartition, List[KafkaRecord]],
      eventSource: String,
      bootstrapServers: List[SocketAddress[Host]]
  ) extends KafkaEvent {
    override def productPrefix = "KafkaEvent"
  }
}

sealed abstract class KafkaRecord {
  def topic: String
  def partition: Int
  def offset: Long
  def timestamp: Instant
  def timestampType: TimestampType
  def key: ByteVector
  def value: ByteVector
  def headers: List[Map[String, ByteVector]]
}

object KafkaRecord {
  def apply(
      topic: String,
      partition: Int,
      offset: Long,
      timestamp: Instant,
      timestampType: TimestampType,
      key: ByteVector,
      value: ByteVector,
      headers: List[Map[String, List[Int]]]
  ): KafkaRecord = {
    val byteHeaders = headers.map { header =>
      header.map { case (k, v) => (k, ByteVector(v.map(_.toByte).toArray)) }
    }
    Impl(topic, partition, offset, timestamp, timestampType, key, value, byteHeaders)
  }

  import codecs.decodeInstant
  import io.circe.scodec.decodeByteVector
  private[events] implicit val decoder: Decoder[KafkaRecord] =
    Decoder.forProduct8(
      "topic",
      "partition",
      "offset",
      "timestamp",
      "timestampType",
      "key",
      "value",
      "headers"
    )(KafkaRecord.apply)

  private final case class Impl(
      topic: String,
      partition: Int,
      offset: Long,
      timestamp: Instant,
      timestampType: TimestampType,
      key: ByteVector,
      value: ByteVector,
      headers: List[Map[String, ByteVector]]
  ) extends KafkaRecord {
    override def productPrefix = "KafkaEventRecord"
  }
}

sealed abstract class TopicPartition {
  def topic: String
  def partition: Int

  override def toString = s"TopicPartition($topic-$partition)"
}

object TopicPartition {
  def apply(
      topic: String,
      partition: Int
  ): TopicPartition =
    Impl(topic, partition)

  private[events] implicit val keyDecoder: KeyDecoder[TopicPartition] = key => {
    key.lastIndexOf("-") match {
      case -1 => None
      case i => Some(TopicPartition(key.substring(0, i), key.substring(i + 1).toInt))
    }
  }

  private final case class Impl(
      topic: String,
      partition: Int
  ) extends TopicPartition {
    override def productPrefix = "TopicPartition"
  }
}

sealed trait TimestampType
case object CREATE_TIME extends TimestampType
case object LOG_APPEND_TIME extends TimestampType

object TimestampType {
  implicit val decoder: Decoder[TimestampType] = Decoder.decodeString.emap {
    case "CREATE_TIME" => Right(CREATE_TIME)
    case "LOG_APPEND_TIME" => Right(LOG_APPEND_TIME)
    case other => Left(s"Unknown timestamp type: $other")
  }
}
