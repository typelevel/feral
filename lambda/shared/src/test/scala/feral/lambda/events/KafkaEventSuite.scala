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
import scodec.bits.ByteVector

import java.time.Instant

class KafkaEventSuite extends FunSuite {

  test("decoder") {
    assertEquals(event.as[KafkaEvent].toTry.get, result)
  }

  def event = json"""
  {
     "eventSource":"aws:kafka",
     "eventSourceArn":"arn:aws:kafka:us-east-1:123456789012:cluster/vpc-2priv-2pub/751d2973-a626-431c-9d4e-d7975eb44dd7-2",
     "bootstrapServers":"b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092,b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092",
     "records":{
        "mytopic-0":[
           {
              "topic":"my-topic",
              "partition":0,
              "offset":15,
              "timestamp":1545084650987,
              "timestampType":"CREATE_TIME",
              "key":"abcDEFghiJKLmnoPQRstuVWXyz1234==",
              "value":"SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==",
              "headers":[
                 {
                    "headerKey":[
                       104,
                       101,
                       97,
                       100,
                       101,
                       114,
                       86,
                       97,
                       108,
                       117,
                       101
                    ]
                 }
              ]
           }
        ]
     }
  }
  """

  def result = KafkaEvent(
    records = Map(
      TopicPartition("mytopic", 0) -> List(KafkaEventRecord(
        topic = "my-topic",
        partition = 0,
        offset = 15,
        timestamp = Instant.ofEpochMilli(1545084650987L),
        timestampType = "CREATE_TIME",
        headers =
          List(Map("headerKey" -> List(104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101))),
        key = ByteVector.fromBase64("abcDEFghiJKLmnoPQRstuVWXyz1234==").get,
        value = ByteVector.fromBase64("SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==").get
      ))),
    eventSource = "aws:kafka",
    eventSourceArn =
      "arn:aws:kafka:us-east-1:123456789012:cluster/vpc-2priv-2pub/751d2973-a626-431c-9d4e-d7975eb44dd7-2",
    bootstrapServers =
      "b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092,b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092"
  )
}
