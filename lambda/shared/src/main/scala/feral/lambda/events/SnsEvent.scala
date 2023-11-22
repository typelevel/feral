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

import cats.syntax.all._
import io.circe.Decoder
import io.circe.jawn.decode
import io.circe.scodec._
import scodec.bits.ByteVector

import java.time.Instant
import java.util.UUID

sealed abstract class SnsEvent {
  def records: List[SnsEventRecord]
}

object SnsEvent {
  def apply(records: List[SnsEventRecord]): SnsEvent =
    new Impl(records)

  implicit val decoder: Decoder[SnsEvent] =
    Decoder.forProduct1("Records")(SnsEvent.apply)

  private final case class Impl(
      records: List[SnsEventRecord]
  ) extends SnsEvent {
    override def productPrefix = "SnsEvent"
  }
}

sealed abstract class SnsEventRecord {
  def eventVersion: String
  def eventSubscriptionArn: String
  def eventSource: String
  def sns: SnsMessage
}

object SnsEventRecord {
  def apply(
      eventVersion: String,
      eventSubscriptionArn: String,
      eventSource: String,
      sns: SnsMessage
  ): SnsEventRecord =
    new Impl(eventVersion, eventSubscriptionArn, eventSource, sns)

  private[events] implicit val decoder: Decoder[SnsEventRecord] = Decoder.forProduct4(
    "EventVersion",
    "EventSubscriptionArn",
    "EventSource",
    "Sns"
  )(SnsEventRecord.apply)

  private final case class Impl(
      eventVersion: String,
      eventSubscriptionArn: String,
      eventSource: String,
      sns: SnsMessage
  ) extends SnsEventRecord {
    override def productPrefix = "SnsEventRecord"
  }
}

sealed abstract class SnsMessage {
  def signature: String
  def messageId: UUID
  def `type`: String
  def topicArn: String
  def messageAttributes: Map[String, SnsMessageAttribute]
  def signatureVersion: String
  def timestamp: Instant
  def signingCertUrl: String
  def message: String
  def unsubscribeUrl: String
  def subject: Option[String]
}

object SnsMessage {

  def apply(
      signature: String,
      messageId: UUID,
      `type`: String,
      topicArn: String,
      messageAttributes: Map[String, SnsMessageAttribute],
      signatureVersion: String,
      timestamp: Instant,
      signingCertUrl: String,
      message: String,
      unsubscribeUrl: String,
      subject: Option[String]
  ): SnsMessage =
    new Impl(
      signature,
      messageId,
      `type`,
      topicArn,
      messageAttributes,
      signatureVersion,
      timestamp,
      signingCertUrl,
      message,
      unsubscribeUrl,
      subject
    )

  private[events] implicit val decoder: Decoder[SnsMessage] = Decoder.forProduct11(
    "Signature",
    "MessageId",
    "Type",
    "TopicArn",
    "MessageAttributes",
    "SignatureVersion",
    "Timestamp",
    "SigningCertUrl",
    "Message",
    "UnsubscribeUrl",
    "Subject"
  )(SnsMessage.apply)

  private final case class Impl(
      signature: String,
      messageId: UUID,
      `type`: String,
      topicArn: String,
      messageAttributes: Map[String, SnsMessageAttribute],
      signatureVersion: String,
      timestamp: Instant,
      signingCertUrl: String,
      message: String,
      unsubscribeUrl: String,
      subject: Option[String]
  ) extends SnsMessage {
    override def productPrefix = "SnsMessage"
  }
}

sealed abstract class SnsMessageAttribute

object SnsMessageAttribute {
  final case class String(value: Predef.String) extends SnsMessageAttribute
  final case class Binary(value: ByteVector) extends SnsMessageAttribute
  final case class Number(value: BigDecimal) extends SnsMessageAttribute
  final case class StringArray(value: List[SnsMessageAttributeArrayMember])
      extends SnsMessageAttribute
  final case class Unknown(
      `type`: Predef.String,
      value: Option[Predef.String]
  ) extends SnsMessageAttribute

  private[events] implicit val decoder: Decoder[SnsMessageAttribute] = {
    val getString: Decoder[Predef.String] = Decoder.instance(_.get[Predef.String]("Value"))
    val getByteVector: Decoder[ByteVector] = Decoder.instance(_.get[ByteVector]("Value"))
    val getNumber: Decoder[BigDecimal] = Decoder.instance(_.get[BigDecimal]("Value"))

    /*
    See https://docs.aws.amazon.com/sns/latest/dg/sns-message-attributes.html

    In particular, for both Number and String.Array:

    > This data type isn't supported for AWS Lambda subscriptions. If you specify this data type for Lambda endpoints,
    > it's passed as the String data type in the JSON payload that Amazon SNS delivers to Lambda."

    Circe is smart enough to handle this for BigDecimal, but no such luck for this particular mess.
     */

    val getStringArray: Decoder[List[SnsMessageAttributeArrayMember]] =
      getString.emapTry(decode[List[SnsMessageAttributeArrayMember]](_).toTry)

    Decoder.instance(_.get[Predef.String]("Type")).flatMap {
      case "String" => getString.map(SnsMessageAttribute.String.apply)
      case "Binary" => getByteVector.map(SnsMessageAttribute.Binary.apply)
      case "Number" => getNumber.map(SnsMessageAttribute.Number.apply)
      case "String.Array" => getStringArray.map(SnsMessageAttribute.StringArray.apply)
      case someType =>
        Decoder.instance(i =>
          i.get[Option[Predef.String]]("Value").map(SnsMessageAttribute.Unknown(someType, _)))
    }
  }
}

sealed abstract class SnsMessageAttributeArrayMember

object SnsMessageAttributeArrayMember {
  final case class String(value: Predef.String) extends SnsMessageAttributeArrayMember
  final case class Number(value: BigDecimal) extends SnsMessageAttributeArrayMember
  final case class Boolean(value: scala.Boolean) extends SnsMessageAttributeArrayMember

  private[events] implicit val decoder: Decoder[SnsMessageAttributeArrayMember] = {
    val bool: Decoder[SnsMessageAttributeArrayMember.Boolean] =
      Decoder.decodeBoolean.map(SnsMessageAttributeArrayMember.Boolean.apply)

    val number: Decoder[SnsMessageAttributeArrayMember.Number] =
      Decoder.decodeBigDecimal.map(SnsMessageAttributeArrayMember.Number.apply)

    val string: Decoder[SnsMessageAttributeArrayMember.String] =
      Decoder.decodeString.map(SnsMessageAttributeArrayMember.String.apply)

    List[Decoder[SnsMessageAttributeArrayMember]](
      bool.widen,
      number.widen,
      string.widen
    ).reduce(_ or _)
  }
}
