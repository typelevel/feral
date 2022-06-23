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

import io.circe.literal._
import io.circe.syntax._
import munit.FunSuite

class S3BatchEventSuite extends FunSuite {

  test("decoder") {
    event.as[S3BatchEvent].toTry.get
    eventNullVersionId.as[S3BatchEvent].toTry.get
  }

  test("result encoder") {
    assertEquals(result.asJson, resultEncoded)
  }

  def event = json"""
  {
    "invocationSchemaVersion": "1.0",
    "invocationId": "YXNkbGZqYWRmaiBhc2RmdW9hZHNmZGpmaGFzbGtkaGZza2RmaAo",
    "job": {
      "id": "f3cc4f60-61f6-4a2b-8a21-d07600c373ce"
    },
    "tasks": [
      {
        "taskId": "dGFza2lkZ29lc2hlcmUK",
        "s3Key": "customerImage1.jpg",
        "s3VersionId": "1",
        "s3BucketArn": "arn:aws:s3:us-east-1:0123456788:awsexamplebucket1"
      }
    ]
  }
  """

  def eventNullVersionId = json"""
  {
    "invocationSchemaVersion": "1.0",
    "invocationId": "YXNkbGZqYWRmaiBhc2RmdW9hZHNmZGpmaGFzbGtkaGZza2RmaAo",
    "job": {
      "id": "f3cc4f60-61f6-4a2b-8a21-d07600c373ce"
    },
    "tasks": [
      {
        "taskId": "dGFza2lkZ29lc2hlcmUK",
        "s3Key": "customerImage1.jpg",
        "s3VersionId": null,
        "s3BucketArn": "arn:aws:s3:us-east-1:0123456788:awsexamplebucket1"
      }
    ]
  }
  """

  def result = S3BatchResult(
    "1.0",
    S3BatchResultResultCode.PermanentFailure,
    "YXNkbGZqYWRmaiBhc2RmdW9hZHNmZGpmaGFzbGtkaGZza2RmaAo",
    List(
      S3BatchResultResult(
        "dGFza2lkZ29lc2hlcmUK",
        S3BatchResultResultCode.Succeeded,
        List("Mary Major", "John Stiles").asJson.noSpaces))
  )

  def resultEncoded = json"""
  {
    "invocationSchemaVersion": "1.0",
    "treatMissingKeysAs" : "PermanentFailure",
    "invocationId" : "YXNkbGZqYWRmaiBhc2RmdW9hZHNmZGpmaGFzbGtkaGZza2RmaAo",
    "results": [
      {
        "taskId": "dGFza2lkZ29lc2hlcmUK",
        "resultCode": "Succeeded",
        "resultString": "[\"Mary Major\",\"John Stiles\"]"
      }
    ]
  }
  """
}
