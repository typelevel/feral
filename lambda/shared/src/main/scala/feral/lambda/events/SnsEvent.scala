package feral.lambda
package events

import cats.syntax.all._
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.scodec._
import scodec.bits.ByteVector

import java.time.Instant
import java.util.UUID
import scala.util.Try

/*
{
  "Records": [
    {
      "EventVersion": "1.0",
      "EventSubscriptionArn": "arn:aws:sns:TEST",
      "EventSource": "aws:sns",
      "Sns": {
        "Signature": "TEST",
        "MessageId": "9f0cfa95-e344-4f93-89ab-5e979503ed1f",
        "Type": "Notification",
        "TopicArn": "arn:aws:sns:TEST",
        "MessageAttributes": {
          "string": {
            "Type": "String",
            "Value": "Testing"
          },
          "binary": {
            "Type": "Binary",
            "Value": "Testing"
          },
          "number": {
            "Type": "Number",
            "Value": 1e6
          },
          "numberString": {
            "Type": "Number",
            "Value": "5"
          },
          "array": {
            "Type": "String.Array",
            "Value": "[1, \"two\", true, false]"
          },
          "unsupported": {
            "Type": "FancyNewType",
            "Value": "SpecialValueHere"
          }
        },
        "SignatureVersion": "1",
        "Timestamp": "2022-04-06T01:02:03.456Z",
        "SigningCertUrl": "TEST",
        "Message": "Testing Message",
        "UnsubscribeUrl": "TEST",
        "Subject": "Test Message"
      }
    }
  ]
}

==>

SnsEvent(
  List(
    SnsRecord(
      "1.0",
      "arn:aws:sns:TEST",
      "aws:sns",
      SnsMessage(
        "TEST",
        9f0cfa95-e344-4f93-89ab-5e979503ed1f,
        "Notification",
        "arn:aws:sns:TEST",
        Map(
          "number" -> Number(1E+6),
          "binary" -> Binary(ByteVector(5 bytes, 0x4deb2d8a78)),
          "string" -> String("Testing"),
          "numberString" -> Number(5),
          "array" -> StringArray(List(Number(1), String("two"), Boolean(true), Boolean(false))),
          "unsupported" -> Unknown("FancyNewType", Some("SpecialValueHere"))
        ),
        "1",
        2022-04-06T01:02:03.456Z,
        "TEST",
        "Testing Message",
        "TEST",
        "Test Message"
      )
    )
  )
)
 */

final case class SnsEvent(
    records: List[SnsEventRecord]
)

object SnsEvent {
  implicit val decoder: Decoder[SnsEvent] =
    Decoder.forProduct1("Records")(SnsEvent.apply)
}

final case class SnsEventRecord(
    eventVersion: String,
    eventSubscriptionArn: String,
    eventSource: String,
    sns: SnsMessage
)

object SnsEventRecord {
  implicit val decoder: Decoder[SnsEventRecord] = Decoder.forProduct4(
    "EventVersion",
    "EventSubscriptionArn",
    "EventSource",
    "Sns"
  )(SnsEventRecord.apply)
}

final case class SnsMessage(
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
    subject: String
)

object SnsMessage {
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try {
      Instant.parse(str)
    }
  }
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
  final case class String(value: Predef.String) extends SnsMessageAttributeArrayMember
  final case class Number(value: BigDecimal) extends SnsMessageAttributeArrayMember
  final case class Boolean(value: scala.Boolean) extends SnsMessageAttributeArrayMember

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
