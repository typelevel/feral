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

package feral.googlecloud
package events

import io.circe._
import scodec.bits.ByteVector

import java.time.Instant

sealed abstract class MessagePublishedData {
  def message: PubsubMessage
  def subscription: String
  def deliveryAttempt: Option[Int]
}

object MessagePublishedData {

  def apply(
      message: PubsubMessage,
      subscription: String,
      deliveryAttempt: Option[Int] = None
  ): MessagePublishedData = {
    new Impl(message, subscription, deliveryAttempt)
  }

  implicit val decoder: Decoder[MessagePublishedData] =
    Decoder.forProduct3("message", "subscription", "deliveryAttempt")(
      MessagePublishedData.apply
    )

  private final case class Impl(
      message: PubsubMessage,
      subscription: String,
      deliveryAttempt: Option[Int]
  ) extends MessagePublishedData {
    override def productPrefix = "MessagePublishedData"
  }
}

sealed abstract class PubsubMessage {
  def data: ByteVector
  def attributes: Map[String, String]
  def messageId: String
  def publishTime: Instant
  def orderingKey: Option[String]
}

object PubsubMessage {

  def apply(
      data: ByteVector,
      attributes: Map[String, String],
      messageId: String,
      publishTime: Instant,
      orderingKey: Option[String]
  ): PubsubMessage = {
    new Impl(data, attributes, messageId, publishTime, orderingKey)
  }

  import codecs.{decodeByteVector, decodeInstant}

  implicit val decoder: Decoder[PubsubMessage] = Decoder.forProduct5(
    "data",
    "attributes",
    "messageId",
    "publishTime",
    "orderingKey"
  )(PubsubMessage.apply)

  private final case class Impl(
      data: ByteVector,
      attributes: Map[String, String],
      messageId: String,
      publishTime: Instant,
      orderingKey: Option[String]
  ) extends PubsubMessage { override def productPrefix = "PubsubMessage" }
}
