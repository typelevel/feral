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

import com.comcast.ip4s.SocketAddress
import io.circe.literal._
import munit.FunSuite
import scodec.bits.ByteVector

import java.time.Instant

class KafkaEventSuite extends FunSuite {

  test("decoderMSKEvent") {
    assertEquals(mksSampleEvent.as[MSKEvent].toTry.get, MSKResult)
  }

  test("decoderSelfManageKafkaEvent") {
    assertEquals(selfManagedKafkaEvent.as[KafkaEvent].toTry.get, selfManagedKafkaResult)
  }

  test("mskEventDecodesAsKafkaEvent") {
    assertEquals(mksSampleEvent.as[KafkaEvent].toTry.get, selfManagedKafkaResult)
  }

  test("topicPartitionDecoder") {
    assertEquals(
      topicPartitionSample.as[Map[TopicPartition, Int]].toTry.get,
      topicPartitionResult)
  }

  def topicPartitionSample = json"""{"my-topic-0":0}"""

  def topicPartitionResult: Map[TopicPartition, Int] = Map(TopicPartition("my-topic", 0) -> 0)

  def selfManagedKafkaEvent = json"""
  {
    "eventSource": "SelfManagedKafka",
    "bootstrapServers":"b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092,b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092",
    "records":{
      "mytopic-0":[
         {
            "topic":"mytopic",
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
}"""

  def selfManagedKafkaResult: KafkaEvent = KafkaEvent(
    records = Map(
      TopicPartition("mytopic", 0) -> List(KafkaRecord(
        topic = "mytopic",
        partition = 0,
        offset = 15,
        timestamp = Instant.ofEpochMilli(1545084650987L),
        timestampType = CREATE_TIME,
        headers =
          List(Map("headerKey" -> List(104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101))),
        key = ByteVector.fromBase64("abcDEFghiJKLmnoPQRstuVWXyz1234==").get,
        value = ByteVector.fromBase64("SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==").get
      ))),
    bootstrapServers = List(
      SocketAddress
        .fromString("b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092")
        .get,
      SocketAddress
        .fromString("b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092")
        .get
    )
  )

  def mksSampleEvent =
    json"""
  {
     "eventSource":"aws:kafka",
     "eventSourceArn":"arn:aws:kafka:us-east-1:123456789012:cluster/vpc-2priv-2pub/751d2973-a626-431c-9d4e-d7975eb44dd7-2",
     "bootstrapServers":"b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092,b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092",
     "records":{
        "mytopic-0":[
           {
              "topic":"mytopic",
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

  def MSKResult: MSKEvent = MSKEvent(
    records = Map(
      TopicPartition("mytopic", 0) -> List(KafkaRecord(
        topic = "mytopic",
        partition = 0,
        offset = 15,
        timestamp = Instant.ofEpochMilli(1545084650987L),
        timestampType = CREATE_TIME,
        headers =
          List(Map("headerKey" -> List(104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101))),
        key = ByteVector.fromBase64("abcDEFghiJKLmnoPQRstuVWXyz1234==").get,
        value = ByteVector.fromBase64("SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==").get
      ))),
    eventSourceArn =
      "arn:aws:kafka:us-east-1:123456789012:cluster/vpc-2priv-2pub/751d2973-a626-431c-9d4e-d7975eb44dd7-2",
    bootstrapServers = List(
      SocketAddress
        .fromString("b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092")
        .get,
      SocketAddress
        .fromString("b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092")
        .get
    )
  )
}
