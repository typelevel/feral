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
import org.typelevel.ci._
import scodec.bits.ByteVector

import java.time.Instant
import scala.util.Try

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/Sqs.d.ts
// https://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html#supported-event-source-Sqs

final case class SqsEvent(
    records: List[SqsRecord]
)

object SqsEvent {
  implicit val decoder: Decoder[SqsEvent] =
    Decoder.instance(_.get[List[SqsRecord]]("Records")).map(SqsEvent(_))
}

final case class SqsRecord(
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

final case class SqsRecordAttributes(
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

  implicit private val instantDecoder: Decoder[Instant] = feral.lambda.events.instantDecoder

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
    Kernel(a.awsTraceHeader.map(`X-Amzn-Trace-Id` -> _).toMap)

  private[this] val `X-Amzn-Trace-Id` = ci"X-Amzn-Trace-Id"
}

sealed abstract class SqsMessageAttribute
object SqsMessageAttribute {
  final case class String(value: Predef.String) extends SqsMessageAttribute

  final case class Binary(value: ByteVector) extends SqsMessageAttribute
  final case class Number(value: BigDecimal) extends SqsMessageAttribute
  final case class Unknown(
      stringValue: Option[Predef.String],
      binaryValue: Option[Predef.String],
      dataType: Predef.String
  ) extends SqsMessageAttribute

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
