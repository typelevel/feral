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
import natchez.Kernel
import scodec.bits.ByteVector

import java.time.Instant
import scala.util.Try

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/Sqs.d.ts
// https://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html#supported-event-source-Sqs

sealed abstract case class SqsEvent private (
    records: List[SqsRecord]
)

object SqsEvent {
  private[lambda] def apply(records: List[SqsRecord]): SqsEvent =
    new SqsEvent(records) {}

  implicit val decoder: Decoder[SqsEvent] =
    Decoder.instance(_.get[List[SqsRecord]]("Records")).map(SqsEvent(_))
}

sealed abstract case class SqsRecord(
    messageId: String,
    receiptHandle: String,
    body: String,
    attributes: SqsRecordAttributes,
    messageAttributes: Map[String, SqsMessageAttribute],
    md5OfBody: String,
    eventSource: String,
    eventSourceArn: String,
    awsRegion: String
)

object SqsRecord {
  private[lambda] def apply(
      messageId: String,
      receiptHandle: String,
      body: String,
      attributes: SqsRecordAttributes,
      messageAttributes: Map[String, SqsMessageAttribute],
      md5OfBody: String,
      eventSource: String,
      eventSourceArn: String,
      awsRegion: String
  ): SqsRecord =
    new SqsRecord(
      messageId,
      receiptHandle,
      body,
      attributes,
      messageAttributes,
      md5OfBody,
      eventSource,
      eventSourceArn,
      awsRegion
    ) {}

  implicit val decoder: Decoder[SqsRecord] = Decoder.instance(i =>
    for {
      messageId <- i.get[String]("messageId")
      receiptHandle <- i.get[String]("receiptHandle")
      body <- i.get[String]("body")
      attributes <- i.get[SqsRecordAttributes]("attributes")
      messageAttributes <- i.get[Map[String, SqsMessageAttribute]]("messageAttributes")
      md5OfBody <- i.get[String]("md5OfBody")
      eventSource <- i.get[String]("eventSource")
      eventSourceArn <- i.get[String]("eventSourceARN")
      awsRegion <- i.get[String]("awsRegion")
    } yield SqsRecord(
      messageId,
      receiptHandle,
      body,
      attributes,
      messageAttributes,
      md5OfBody,
      eventSource,
      eventSourceArn,
      awsRegion
    ))
}

sealed abstract case class SqsRecordAttributes private (
    awsTraceHeader: Option[String],
    approximateReceiveCount: String,
    sentTimestamp: Instant,
    senderId: String,
    approximateFirstReceiveTimestamp: Instant,
    sequenceNumber: Option[String],
    messageGroupId: Option[String],
    messageDeduplicationId: Option[String]
)

object SqsRecordAttributes {
  private[lambda] def apply(
      awsTraceHeader: Option[String],
      approximateReceiveCount: String,
      sentTimestamp: Instant,
      senderId: String,
      approximateFirstReceiveTimestamp: Instant,
      sequenceNumber: Option[String],
      messageGroupId: Option[String],
      messageDeduplicationId: Option[String]
  ): SqsRecordAttributes =
    new SqsRecordAttributes(
      awsTraceHeader,
      approximateReceiveCount,
      sentTimestamp,
      senderId,
      approximateFirstReceiveTimestamp,
      sequenceNumber,
      messageGroupId,
      messageDeduplicationId
    ) {}

  implicit val decoder: Decoder[SqsRecordAttributes] = Decoder.instance(i =>
    for {
      awsTraceHeader <- i.get[Option[String]]("AWSTraceHeader")
      approximateReceiveCount <- i.get[String]("ApproximateReceiveCount")
      sentTimestamp <- i.get[Instant]("SentTimestamp")
      senderId <- i.get[String]("SenderId")
      approximateFirstReceiveTimestamp <- i.get[Instant]("ApproximateFirstReceiveTimestamp")
      sequenceNumber <- i.get[Option[String]]("SequenceNumber")
      messageGroupId <- i.get[Option[String]]("MessageGroupId")
      messageDeduplicationId <- i.get[Option[String]]("MessageDeduplicationId")
    } yield SqsRecordAttributes(
      awsTraceHeader,
      approximateReceiveCount,
      sentTimestamp,
      senderId,
      approximateFirstReceiveTimestamp,
      sequenceNumber,
      messageGroupId,
      messageDeduplicationId
    ))

  implicit def kernelSource: KernelSource[SqsRecordAttributes] = a =>
    Kernel(a.awsTraceHeader.map("X-Amzn-Trace-Id" -> _).toMap)
}

sealed abstract class SqsMessageAttribute extends Product with Serializable
object SqsMessageAttribute {
  sealed abstract case class String private (value: Predef.String) extends SqsMessageAttribute
  object String {
    private[lambda] def apply(value: Predef.String): String =
      new String(value) {}
  }

  sealed abstract case class Binary private (value: ByteVector) extends SqsMessageAttribute
  object Binary {
    private[lambda] def apply(value: ByteVector): Binary =
      new Binary(value) {}
  }

  sealed abstract case class Number private (value: BigDecimal) extends SqsMessageAttribute
  object Number {
    private[lambda] def apply(value: BigDecimal): Number =
      new Number(value) {}
  }

  sealed abstract case class Unknown(
      stringValue: Option[Predef.String],
      binaryValue: Option[Predef.String],
      dataType: Predef.String
  ) extends SqsMessageAttribute
  object Unknown {
    private[lambda] def apply(
        stringValue: Option[Predef.String],
        binaryValue: Option[Predef.String],
        dataType: Predef.String
    ): Unknown = new Unknown(stringValue, binaryValue, dataType) {}
  }

  implicit val decoder: Decoder[SqsMessageAttribute] = {
    val strValue =
      Decoder.instance(_.get[Predef.String]("stringValue"))

    val binValue =
      Decoder.instance(_.get[ByteVector]("binaryValue"))

    Decoder.instance(_.get[Predef.String]("dataType")).flatMap {
      case "String" => strValue.map(SqsMessageAttribute.String(_): SqsMessageAttribute)
      case "Binary" => binValue.map(SqsMessageAttribute.Binary(_))
      case "Number" =>
        strValue.emapTry(n => Try(BigDecimal(n))).map(SqsMessageAttribute.Number(_))
      case dataType =>
        Decoder.instance(i =>
          for {
            str <- i.get[Option[Predef.String]]("stringValue")
            bin <- i.get[Option[Predef.String]]("binaryValue")
          } yield Unknown(str, bin, dataType))
    }
  }
}
