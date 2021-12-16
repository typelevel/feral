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

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/sqs.d.ts
// https://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html#supported-event-source-sqs

final case class SQSEvent(
    records: List[SQSRecord]
)

object SQSEvent {
  implicit val decoder: Decoder[SQSEvent] =
    Decoder.instance(_.get[List[SQSRecord]]("Records")).map(SQSEvent(_))
}

final case class SQSRecord(
    messageId: String,
    receiptHandle: String,
    body: String,
    attributes: SQSRecordAttributes,
    messageAttributes: Map[String, SQSMessageAttribute],
    md5OfBody: String,
    eventSource: String,
    eventSourceARN: String,
    awsRegion: String
)

object SQSRecord {
  implicit val decoder: Decoder[SQSRecord] = Decoder.instance(i =>
    for {
      messageId <- i.get[String]("messageId")
      receiptHandle <- i.get[String]("receiptHandle")
      body <- i.get[String]("body")
      attributes <- i.get[SQSRecordAttributes]("attributes")
      messageAttributes <- i.get[Map[String, SQSMessageAttribute]]("messageAttributes")
      md5OfBody <- i.get[String]("md5OfBody")
      eventSource <- i.get[String]("eventSource")
      eventSourceARN <- i.get[String]("eventSourceARN")
      awsRegion <- i.get[String]("awsRegion")
    } yield SQSRecord(
      messageId,
      receiptHandle,
      body,
      attributes,
      messageAttributes,
      md5OfBody,
      eventSource,
      eventSourceARN,
      awsRegion
    ))
}

final case class SQSRecordAttributes(
    awsTraceHeader: Option[String],
    approximateReceiveCount: String,
    sentTimestamp: Instant,
    senderId: String,
    approximateFirstReceiveTimestamp: Instant,
    sequenceNumber: Option[String],
    messageGroupId: Option[String],
    messageDeduplicationId: Option[String]
)

object SQSRecordAttributes {

  implicit val decoder: Decoder[SQSRecordAttributes] = Decoder.instance(i =>
    for {
      awsTraceHeader <- i.get[Option[String]]("AWSTraceHeader")
      approximateReceiveCount <- i.get[String]("ApproximateReceiveCount")
      sentTimestamp <- i.get[Instant]("SentTimestamp")
      senderId <- i.get[String]("SenderId")
      approximateFirstReceiveTimestamp <- i.get[Instant]("ApproximateFirstReceiveTimestamp")
      sequenceNumber <- i.get[Option[String]]("SequenceNumber")
      messageGroupId <- i.get[Option[String]]("MessageGroupId")
      messageDeduplicationId <- i.get[Option[String]]("MessageDeduplicationId")
    } yield SQSRecordAttributes(
      awsTraceHeader,
      approximateReceiveCount,
      sentTimestamp,
      senderId,
      approximateFirstReceiveTimestamp,
      sequenceNumber,
      messageGroupId,
      messageDeduplicationId
    ))

  implicit def kernelSource: KernelSource[SQSRecordAttributes] = a =>
    Kernel(a.awsTraceHeader.map("X-Amzn-Trace-Id" -> _).toMap)
}

sealed abstract class SQSMessageAttribute
object SQSMessageAttribute {
  final case class String(value: Predef.String) extends SQSMessageAttribute

  final case class Binary(value: ByteVector) extends SQSMessageAttribute
  final case class Number(value: BigDecimal) extends SQSMessageAttribute
  final case class Unknown(
      stringValue: Option[Predef.String],
      binaryValue: Option[Predef.String],
      dataType: Predef.String
  ) extends SQSMessageAttribute

  implicit val decoder: Decoder[SQSMessageAttribute] = {
    val strValue =
      Decoder.instance(_.get[Predef.String]("stringValue"))

    val binValue =
      Decoder.instance(_.get[ByteVector]("binaryValue"))

    Decoder.instance(_.get[Predef.String]("dataType")).flatMap {
      case "String" => strValue.map(SQSMessageAttribute.String(_): SQSMessageAttribute)
      case "Binary" => binValue.map(SQSMessageAttribute.Binary)
      case "Number" =>
        strValue.emapTry(n => Try(BigDecimal(n))).map(SQSMessageAttribute.Number)
      case dataType =>
        Decoder.instance(i =>
          for {
            str <- i.get[Option[Predef.String]]("stringValue")
            bin <- i.get[Option[Predef.String]]("binaryValue")
          } yield Unknown(str, bin, dataType))
    }
  }
}