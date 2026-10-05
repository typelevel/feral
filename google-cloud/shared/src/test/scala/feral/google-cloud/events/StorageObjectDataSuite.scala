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

package feral.googlecloud.events

import io.circe.literal._
import munit.FunSuite

import java.time.Instant

class StorageObjectDataSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[StorageObjectData].toTry.get, result)
  }

  def event = json"""
    {
      "bucket": "some-bucket",
      "cacheControl": "",
      "componentCount": 0,
      "contentDisposition": "",
      "contentEncoding": "",
      "contentLanguage": "",
      "contentType": "text/plain",
      "crc32c": "rTVTeQ==",
      "customerEncryption": {
          "encryptionAlgorithm": "AES256",
          "keySha256": "abc123"
      },
      "etag": "CNHZkbuF/ugCEAE=",
      "eventBasedHold": false,
      "generation": 12345,
      "id": "some-bucket/folder/Test.cs/1587627537231057",
      "kind": "storage#object",
      "kmsKeyName": "",
      "md5Hash": "kF8MuJ5+CTJxvyhHS1xzRg==",
      "mediaLink": "https://www.googleapis.com/download/storage/v1/b/some-bucket/o/folder%2FTest.cs?generation=1587627537231057\u0026alt=media",
      "metadata": {},
      "metageneration": 1,
      "name": "folder/Test.cs",
      "retentionExpirationTime": "",
      "selfLink": "https://www.googleapis.com/storage/v1/b/some-bucket/o/folder/Test.cs",
      "size": 352,
      "storageClass": "MULTI_REGIONAL",
      "temporaryHold": false,
      "timeCreated": "2020-04-23T07:38:57.230Z",
      "timeDeleted": "",
      "timeStorageClassUpdated": "2020-04-23T07:38:57.230Z",
      "updated": "2020-04-23T07:38:57.230Z"
    }
  """

  def result = StorageObjectData(
    contentEncoding = "",
    contentDisposition = "",
    cacheControl = "",
    contentLanguage = "",
    metageneration = 1,
    timeDeleted = None,
    contentType = "text/plain",
    size = 352,
    timeCreated = Some(Instant.parse("2020-04-23T07:38:57.230Z")),
    crc32c = "rTVTeQ==",
    componentCount = 0,
    md5Hash = "kF8MuJ5+CTJxvyhHS1xzRg==",
    etag = "CNHZkbuF/ugCEAE=",
    updated = Some(Instant.parse("2020-04-23T07:38:57.230Z")),
    storageClass = "MULTI_REGIONAL",
    kmsKeyName = "",
    timeStorageClassUpdated = Some(Instant.parse("2020-04-23T07:38:57.230Z")),
    temporaryHold = false,
    retentionExpirationTime = None,
    metadata = Map.empty,
    eventBasedHold = false,
    name = "folder/Test.cs",
    id = "some-bucket/folder/Test.cs/1587627537231057",
    bucket = "some-bucket",
    generation = 12345,
    customerEncryption = CustomerEncryption(
      encryptionAlgorithm = "AES256",
      keySha256 = "abc123"
    ),
    mediaLink =
      "https://www.googleapis.com/download/storage/v1/b/some-bucket/o/folder%2FTest.cs?generation=1587627537231057\u0026alt=media",
    selfLink = "https://www.googleapis.com/storage/v1/b/some-bucket/o/folder/Test.cs",
    kind = "storage#object"
  )

}
