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

import com.comcast.ip4s.IpAddress
import io.circe.Decoder

import java.time.Instant

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/s3.d.ts

sealed abstract class S3Event {
  def records: List[S3EventRecord]
}

object S3Event {
  def apply(records: List[S3EventRecord]): S3Event =
    new Impl(records)

  implicit val decoder: Decoder[S3Event] = Decoder.forProduct1("Records")(S3Event.apply)

  private final case class Impl(records: List[S3EventRecord]) extends S3Event {
    override def productPrefix = "S3Event"
  }
}

sealed abstract class S3EventRecord {
  def eventVersion: String
  def eventSource: String
  def awsRegion: String
  def eventTime: Instant
  def eventName: String
  def userIdentity: S3UserIdentity
  def requestParameters: S3RequestParameters
  def responseElements: S3ResponseElements
  def s3: S3
  def glacierEventData: Option[S3EventRecordGlacierEventData]
}

object S3EventRecord {

  def apply(
      eventVersion: String,
      eventSource: String,
      awsRegion: String,
      eventTime: Instant,
      eventName: String,
      userIdentity: S3UserIdentity,
      requestParameters: S3RequestParameters,
      responseElements: S3ResponseElements,
      s3: S3,
      glacierEventData: Option[S3EventRecordGlacierEventData]
  ): S3EventRecord =
    new Impl(
      eventVersion,
      eventSource,
      awsRegion,
      eventTime,
      eventName,
      userIdentity,
      requestParameters,
      responseElements,
      s3,
      glacierEventData
    )

  private[events] implicit val decoder: Decoder[S3EventRecord] =
    Decoder.forProduct10(
      "eventVersion",
      "eventSource",
      "awsRegion",
      "eventTime",
      "eventName",
      "userIdentity",
      "requestParameters",
      "responseElements",
      "s3",
      "glacierEventData"
    )(S3EventRecord.apply)

  private final case class Impl(
      eventVersion: String,
      eventSource: String,
      awsRegion: String,
      eventTime: Instant,
      eventName: String,
      userIdentity: S3UserIdentity,
      requestParameters: S3RequestParameters,
      responseElements: S3ResponseElements,
      s3: S3,
      glacierEventData: Option[S3EventRecordGlacierEventData])
      extends S3EventRecord {
    override def productPrefix = "S3EventRecord"
  }
}

sealed abstract class S3UserIdentity { def principalId: String }

object S3UserIdentity {

  def apply(principalId: String): S3UserIdentity =
    new Impl(principalId)

  private[events] implicit val decoder: Decoder[S3UserIdentity] =
    Decoder.forProduct1("principalId")(S3UserIdentity.apply)

  private final case class Impl(principalId: String) extends S3UserIdentity {
    override def productPrefix = "S3UserIdentity"
  }
}

sealed abstract class S3RequestParameters {
  def sourceIpAddress: IpAddress
}

object S3RequestParameters {

  def apply(sourceIpAddress: IpAddress): S3RequestParameters =
    new Impl(sourceIpAddress)

  import codecs.decodeIpAddress
  private[events] implicit val decoder: Decoder[S3RequestParameters] =
    Decoder.forProduct1("sourceIPAddress")(S3RequestParameters.apply)

  private final case class Impl(sourceIpAddress: IpAddress) extends S3RequestParameters {
    override def productPrefix = "S3RequestParameters"
  }
}

sealed abstract class S3ResponseElements {
  def `x-amz-request-id`: String
  def `x-amz-id-2`: String
}

object S3ResponseElements {

  def apply(`x-amz-request-id`: String, `x-amz-id-2`: String): S3ResponseElements =
    new Impl(`x-amz-request-id`, `x-amz-id-2`)

  private[events] implicit val decoder: Decoder[S3ResponseElements] =
    Decoder.forProduct2("x-amz-request-id", "x-amz-id-2")(S3ResponseElements.apply)

  private final case class Impl(`x-amz-request-id`: String, `x-amz-id-2`: String)
      extends S3ResponseElements {
    override def productPrefix = "S3ResponseElements"
  }

}

sealed abstract class S3 {
  def s3SchemaVersion: String
  def configurationId: String
  def bucket: S3Bucket
  def `object`: S3Object
}

object S3 {

  def apply(
      s3SchemaVersion: String,
      configurationId: String,
      bucket: S3Bucket,
      `object`: S3Object
  ): S3 =
    new Impl(s3SchemaVersion, configurationId, bucket, `object`)

  private[events] implicit val decoder: Decoder[S3] =
    Decoder.forProduct4("s3SchemaVersion", "configurationId", "bucket", "object")(S3.apply)

  private final case class Impl(
      s3SchemaVersion: String,
      configurationId: String,
      bucket: S3Bucket,
      `object`: S3Object)
      extends S3 {
    override def productPrefix = "S3"
  }
}

sealed abstract class S3Bucket {
  def name: String
  def ownerIdentity: S3UserIdentity
  def arn: String
}

object S3Bucket {

  def apply(name: String, ownerIdentity: S3UserIdentity, arn: String): S3Bucket =
    new Impl(name, ownerIdentity, arn)

  private[events] implicit val decoder: Decoder[S3Bucket] =
    Decoder.forProduct3("name", "ownerIdentity", "arn")(S3Bucket.apply)

  private final case class Impl(name: String, ownerIdentity: S3UserIdentity, arn: String)
      extends S3Bucket {
    override def productPrefix = "S3Bucket"
  }
}

sealed abstract class S3Object {
  def key: String
  def size: Long
  def eTag: String
  def versionId: Option[String]
  def sequencer: String
}

object S3Object {

  def apply(
      key: String,
      size: Long,
      eTag: String,
      versionId: Option[String],
      sequencer: String
  ): S3Object =
    new Impl(key, size, eTag, versionId, sequencer)

  private[events] implicit val decoder: Decoder[S3Object] =
    Decoder.forProduct5("key", "size", "eTag", "versionId", "sequencer")(S3Object.apply)

  private final case class Impl(
      key: String,
      size: Long,
      eTag: String,
      versionId: Option[String],
      sequencer: String)
      extends S3Object {
    override def productPrefix = "S3Object"
  }
}

sealed abstract class S3EventRecordGlacierEventData {
  def restoreEventData: S3EventRecordGlacierRestoreEventData
}

object S3EventRecordGlacierEventData {

  def apply(
      restoreEventData: S3EventRecordGlacierRestoreEventData): S3EventRecordGlacierEventData =
    new Impl(restoreEventData)

  private[events] implicit val decoder: Decoder[S3EventRecordGlacierEventData] =
    Decoder.forProduct1("restoreEventData")(S3EventRecordGlacierEventData.apply)

  private final case class Impl(restoreEventData: S3EventRecordGlacierRestoreEventData)
      extends S3EventRecordGlacierEventData {
    override def productPrefix = "S3EventRecordGlacierEventData"
  }
}

sealed abstract class S3EventRecordGlacierRestoreEventData {
  def lifecycleRestorationExpiryTime: Instant
  def lifecycleRestoreStorageClass: String
}

object S3EventRecordGlacierRestoreEventData {

  def apply(
      lifecycleRestorationExpiryTime: Instant,
      lifecycleRestoreStorageClass: String
  ): S3EventRecordGlacierRestoreEventData =
    new Impl(lifecycleRestorationExpiryTime, lifecycleRestoreStorageClass)

  private[events] implicit val decoder: Decoder[S3EventRecordGlacierRestoreEventData] =
    Decoder.forProduct2("lifecycleRestorationExpiryTime", "lifecycleRestoreStorageClass")(
      S3EventRecordGlacierRestoreEventData.apply
    )

  private final case class Impl(
      lifecycleRestorationExpiryTime: Instant,
      lifecycleRestoreStorageClass: String
  ) extends S3EventRecordGlacierRestoreEventData {
    override def productPrefix = "S3EventRecordGlacierRestoreEventData"
  }

}
