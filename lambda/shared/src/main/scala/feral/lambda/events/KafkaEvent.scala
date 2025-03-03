/*
 * Copyright 2025 Typelevel
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

import io.circe.{Decoder, KeyDecoder}
import scodec.bits.ByteVector

import java.time.Instant

sealed abstract class KafkaEvent {
  def records: Map[TopicPartition, List[KafkaEventRecord]]
  def eventSource: String
  def eventSourceArn: String
  def bootstrapServers: String
}

object KafkaEvent {
  def apply(
      records: Map[TopicPartition, List[KafkaEventRecord]],
      eventSource: String,
      eventSourceArn: String,
      bootstrapServers: String
  ): KafkaEvent =
    Impl(records, eventSource, eventSourceArn, bootstrapServers)

  private[events] implicit val decoder: Decoder[KafkaEvent] =
    Decoder.forProduct4(
      "records",
      "eventSource",
      "eventSourceArn",
      "bootstrapServers"
    )(KafkaEvent.apply)

  private final case class Impl(
      records: Map[TopicPartition, List[KafkaEventRecord]],
      eventSource: String,
      eventSourceArn: String,
      bootstrapServers: String
  ) extends KafkaEvent {
    override def productPrefix = "KafkaEvent"
  }
}

sealed abstract class KafkaEventRecord {
  def topic: String
  def partition: Int
  def offset: Long
  def timestamp: Instant
  def timestampType: String
  def key: ByteVector
  def value: ByteVector
  def headers: List[Map[String, ByteVector]]
}

object KafkaEventRecord {
  def apply(
      topic: String,
      partition: Int,
      offset: Long,
      timestamp: Instant,
      timestampType: String,
      key: ByteVector,
      value: ByteVector,
      headers: List[Map[String, List[Int]]]
  ): KafkaEventRecord = {
    val byteHeaders = headers.map { header =>
      header.map { case (k, v) => (k, ByteVector(v.map(_.toByte).toArray)) }
    }
    Impl(topic, partition, offset, timestamp, timestampType, key, value, byteHeaders)
  }

  import codecs.decodeInstant
  import io.circe.scodec.decodeByteVector
  private[events] implicit val decoder: Decoder[KafkaEventRecord] =
    Decoder.forProduct8(
      "topic",
      "partition",
      "offset",
      "timestamp",
      "timestampType",
      "key",
      "value",
      "headers"
    )(KafkaEventRecord.apply)

  private final case class Impl(
      topic: String,
      partition: Int,
      offset: Long,
      timestamp: Instant,
      timestampType: String,
      key: ByteVector,
      value: ByteVector,
      headers: List[Map[String, ByteVector]]
  ) extends KafkaEventRecord {
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
