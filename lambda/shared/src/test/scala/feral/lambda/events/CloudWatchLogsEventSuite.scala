/*
 * Copyright 2025 Typelevel
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

import cats.effect.IO
import io.circe.Json
import io.circe.jawn.decode
import munit.CatsEffectSuite
import scodec.bits.ByteVector
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import scala.io.Source

class CloudWatchLogsEventSuite extends CatsEffectSuite {

  private val sampleDecodedData = CloudWatchLogsDecodedData(
    owner = "123456789123",
    logGroup = "testLogGroup",
    logStream = "testLogStream",
    subscriptionFilters = List("testFilter"),
    messageType = "DATA_MESSAGE",
    logEvents = List(
      CloudWatchLogsLogEvent(
        id = "eventId1",
        timestamp = 1440442987000L,
        message = "[ERROR] First test message",
        extractedFields = None
      ),
      CloudWatchLogsLogEvent(
        id = "eventId2", 
        timestamp = 1440442987001L,
        message = "[ERROR] Second test message",
        extractedFields = None
      )
    )
  )

  private val sampleEventJson = Json.obj(
    "awslogs" -> Json.obj(
      "data" -> Json.fromString("H4sIAAAAAAAAAHWPwQqCQBCGX0Xm7EFtK+smZBEUgXoLCdMhFtKV3akI8d0bLYmibvPPN3wz00CJxmQnTO41whwWQRIctmEcB6sQbFC3CjW3XW8kxpOpP+OC22d1Wml1qZkQGtoMsScxaczKN3plG8zlaHIta5KqWsozoTYw3/djzwhpLwivWFGHGpAFe7DL68JlBUk+l7KSN7tCOEJ4M3/qOI49vMHj+zCKdlFqLaU2ZHV2a4Ct/an0/ivdX8oYc1UVX860fQDQiMdxRQEAAA==")
    )
  )

  test("CloudWatchLogsEvent should decode from JSON") {
    val result = decode[CloudWatchLogsEvent](sampleEventJson.noSpaces)
    assertEquals(
      result,
      Right(CloudWatchLogsEvent(CloudWatchLogsEventData("H4sIAAAAAAAAAHWPwQqCQBCGX0Xm7EFtK+smZBEUgXoLCdMhFtKV3akI8d0bLYmibvPPN3wz00CJxmQnTO41whwWQRIctmEcB6sQbFC3CjW3XW8kxpOpP+OC22d1Wml1qZkQGtoMsScxaczKN3plG8zlaHIta5KqWsozoTYw3/djzwhpLwivWFGHGpAFe7DL68JlBUk+l7KSN7tCOEJ4M3/qOI49vMHj+zCKdlFqLaU2ZHV2a4Ct/an0/ivdX8oYc1UVX860fQDQiMdxRQEAAA==")))
    )
  }

  test("CloudWatchLogsEvent should decode base64 and gzip data into CloudWatchLogsDecodedData") {
    val testIO = for {
      event <- IO.fromEither(decode[CloudWatchLogsEvent](sampleEventJson.noSpaces))
      decodedData <- event.awslogs.data match {
        case data =>
          ByteVector.fromBase64(data) match {
            case Some(bytes) =>
              IO {
                val bis = new ByteArrayInputStream(bytes.toArray)
                val gzis = new GZIPInputStream(bis)
                val decompressed = Source.fromInputStream(gzis, "UTF-8").mkString
                gzis.close()
                decode[CloudWatchLogsDecodedData](decompressed)
              }.flatMap(IO.fromEither)
            case None => IO.raiseError(new IllegalArgumentException("Invalid base64 data"))
          }
      }
      _ <- IO {
        assertEquals(decodedData.owner, sampleDecodedData.owner)
        assertEquals(decodedData.logGroup, sampleDecodedData.logGroup)
        assertEquals(decodedData.logStream, sampleDecodedData.logStream)
        assertEquals(decodedData.subscriptionFilters, sampleDecodedData.subscriptionFilters)
        assertEquals(decodedData.messageType, sampleDecodedData.messageType)
        assertEquals(decodedData.logEvents.size, sampleDecodedData.logEvents.size)
        
        sampleDecodedData.logEvents.zip(decodedData.logEvents).foreach { case (expected, actual) =>
          assertEquals(actual.id, expected.id)
          assertEquals(actual.timestamp, expected.timestamp)
          assertEquals(actual.message, expected.message)
          assertEquals(actual.extractedFields, expected.extractedFields)
        }
      }
    } yield ()

    testIO
  }
}
