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
import munit.FunSuite

import java.time.Instant

class S3EventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[S3Event].toTry.get, result)
  }

  def event = json"""
  {
     "Records":[
        {
           "eventVersion":"2.1",
           "eventSource":"aws:s3",
           "awsRegion":"us-west-2",
           "eventTime":"1970-01-01T00:00:00.000Z",
           "eventName":"ObjectCreated:Put",
           "userIdentity":{
              "principalId":"AIDAJDPLRKLG7UEXAMPLE"
           },
           "requestParameters":{
              "sourceIPAddress":"127.0.0.1"
           },
           "responseElements":{
              "x-amz-request-id":"C3D13FE58DE4C810",
              "x-amz-id-2":"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD"
           },
           "s3":{
              "s3SchemaVersion":"1.0",
              "configurationId":"testConfigRule",
              "bucket":{
                 "name":"mybucket",
                 "ownerIdentity":{
                    "principalId":"A3NL1KOZZKExample"
                 },
                 "arn":"arn:aws:s3:::mybucket"
              },
              "object":{
                 "key":"HappyFace.jpg",
                 "size":1024,
                 "eTag":"d41d8cd98f00b204e9800998ecf8427e",
                 "versionId":"096fKKXTRTtl3on89fVO.nfljtsv6qko",
                 "sequencer":"0055AED6DCD90281E5"
              }
           }
        }
     ]
  }
  """

  def result = S3Event(records = List(
    S3EventRecord(
      eventVersion = "2.1",
      eventSource = "aws:s3",
      awsRegion = "us-west-2",
      eventTime = Instant.ofEpochSecond(0),
      eventName = "ObjectCreated:Put",
      userIdentity = S3UserIdentity("AIDAJDPLRKLG7UEXAMPLE"),
      requestParameters = S3RequestParameters("127.0.0.1"),
      responseElements = S3ResponseElements(
        "C3D13FE58DE4C810",
        "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD"
      ),
      s3 = S3(
        s3SchemaVersion = "1.0",
        configurationId = "testConfigRule": String,
        bucket = S3Bucket(
          name = "mybucket",
          ownerIdentity = S3UserIdentity("A3NL1KOZZKExample"),
          arn = "arn:aws:s3:::mybucket"
        ),
        `object` = S3Object(
          key = "HappyFace.jpg",
          size = 1024L,
          eTag = "d41d8cd98f00b204e9800998ecf8427e",
          versionId = Option("096fKKXTRTtl3on89fVO.nfljtsv6qko"),
          sequencer = "0055AED6DCD90281E5"
        )
      ),
      glacierEventData = None
    )
  ))

}
