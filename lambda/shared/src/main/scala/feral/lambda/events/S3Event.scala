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

import io.circe.Decoder

import java.time.Instant

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/s3.d.ts

final case class S3Event(records: List[S3EventRecord])

object S3Event {
  implicit val decoder: Decoder[S3Event] = Decoder.forProduct1("Records")(S3Event.apply)
}

final case class S3EventRecord(
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

object S3EventRecord {

  implicit val decoder: Decoder[S3EventRecord] =
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

}

final case class S3UserIdentity(principalId: String)

object S3UserIdentity {

  implicit val decoder: Decoder[S3UserIdentity] =
    Decoder.forProduct1("principalId")(S3UserIdentity.apply)

}

final case class S3RequestParameters(sourceIPAddress: String)

object S3RequestParameters {

  implicit val decoder: Decoder[S3RequestParameters] =
    Decoder.forProduct1("sourceIPAddress")(S3RequestParameters.apply)

}

final case class S3ResponseElements(`x-amz-request-id`: String, `x-amz-id-2`: String)

object S3ResponseElements {

  implicit val decoder: Decoder[S3ResponseElements] =
    Decoder.forProduct2("x-amz-request-id", "x-amz-id-2")(S3ResponseElements.apply)

}

final case class S3(
    s3SchemaVersion: String,
    configurationId: String,
    bucket: S3Bucket,
    `object`: S3Object)

object S3 {

  implicit val decoder: Decoder[S3] =
    Decoder.forProduct4("s3SchemaVersion", "configurationId", "bucket", "object")(S3.apply)

}

final case class S3Bucket(name: String, ownerIdentity: S3UserIdentity, arn: String)

object S3Bucket {

  implicit val decoder: Decoder[S3Bucket] =
    Decoder.forProduct3("name", "ownerIdentity", "arn")(S3Bucket.apply)

}

final case class S3Object(
    key: String,
    size: Long,
    eTag: String,
    versionId: Option[String],
    sequencer: String)

object S3Object {

  implicit val decoder: Decoder[S3Object] =
    Decoder.forProduct5("key", "size", "eTag", "versionId", "sequencer")(S3Object.apply)

}

final case class S3EventRecordGlacierEventData(
    restoreEventData: S3EventRecordGlacierRestoreEventData)

object S3EventRecordGlacierEventData {

  implicit val decoder: Decoder[S3EventRecordGlacierEventData] =
    Decoder.forProduct1("restoreEventData")(S3EventRecordGlacierEventData.apply)

}

final case class S3EventRecordGlacierRestoreEventData(
    lifecycleRestorationExpiryTime: Instant,
    lifecycleRestoreStorageClass: String)

object S3EventRecordGlacierRestoreEventData {

  implicit val decoder: Decoder[S3EventRecordGlacierRestoreEventData] =
    Decoder.forProduct2("lifecycleRestorationExpiryTime", "lifecycleRestoreStorageClass")(
      S3EventRecordGlacierRestoreEventData.apply
    )

}
