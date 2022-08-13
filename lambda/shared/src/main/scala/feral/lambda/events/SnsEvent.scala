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

sealed abstract case class SnsEvent private (
    records: List[SnsEventRecord]
)

object SnsEvent {
  private[lambda] def apply(records: List[SnsEventRecord]): SnsEvent =
    new SnsEvent(records) {}

  implicit val decoder: Decoder[SnsEvent] =
    Decoder.forProduct1("Records")(SnsEvent.apply)
}

sealed abstract case class SnsEventRecord private (
    eventVersion: String,
    eventSubscriptionArn: String,
    eventSource: String,
    sns: SnsMessage
)

object SnsEventRecord {
  private[lambda] def apply(
      eventVersion: String,
      eventSubscriptionArn: String,
      eventSource: String,
      sns: SnsMessage
  ): SnsEventRecord =
    new SnsEventRecord(eventVersion, eventSubscriptionArn, eventSource, sns) {}

  implicit val decoder: Decoder[SnsEventRecord] = Decoder.forProduct4(
    "EventVersion",
    "EventSubscriptionArn",
    "EventSource",
    "Sns"
  )(SnsEventRecord.apply)
}

sealed abstract case class SnsMessage private (
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
)

object SnsMessage {
  private[lambda] def apply(
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
    new SnsMessage(
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
    ) {}

  private[this] implicit val instantDecoder: Decoder[Instant] = Decoder.decodeInstant

  implicit val decoder: Decoder[SnsMessage] = Decoder.forProduct11(
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
}

sealed abstract class SnsMessageAttribute extends Product with Serializable

object SnsMessageAttribute {
  sealed abstract case class String private (value: Predef.String) extends SnsMessageAttribute
  object String {
    private[lambda] def apply(value: Predef.String): String =
      new String(value) {}
  }

  sealed abstract case class Binary private (value: ByteVector) extends SnsMessageAttribute
  object Binary {
    private[lambda] def apply(value: ByteVector): Binary =
      new Binary(value) {}
  }

  sealed abstract case class Number private (value: BigDecimal) extends SnsMessageAttribute
  object Number {
    private[lambda] def apply(value: BigDecimal): Number =
      new Number(value) {}
  }

  sealed abstract case class StringArray private (value: List[SnsMessageAttributeArrayMember])
      extends SnsMessageAttribute
  object StringArray {
    private[lambda] def apply(value: List[SnsMessageAttributeArrayMember]): StringArray =
      new StringArray(value) {}
  }

  sealed abstract case class Unknown private (
      `type`: Predef.String,
      value: Option[Predef.String]
  ) extends SnsMessageAttribute
  object Unknown {
    private[lambda] def apply(
        `type`: Predef.String,
        value: Option[Predef.String]
    ): Unknown = new Unknown(`type`, value) {}
  }

  implicit val decoder: Decoder[SnsMessageAttribute] = {
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
  sealed abstract case class String private (value: Predef.String)
      extends SnsMessageAttributeArrayMember
  object String {
    private[lambda] def apply(value: Predef.String): String =
      new String(value) {}
  }

  sealed abstract case class Number private (value: BigDecimal)
      extends SnsMessageAttributeArrayMember
  object Number {
    private[lambda] def apply(value: BigDecimal): Number =
      new Number(value) {}
  }

  sealed abstract case class Boolean private (value: scala.Boolean)
      extends SnsMessageAttributeArrayMember
  object Boolean {
    private[lambda] def apply(value: scala.Boolean): Boolean =
      new Boolean(value) {}
  }

  implicit val decoder: Decoder[SnsMessageAttributeArrayMember] = {
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
