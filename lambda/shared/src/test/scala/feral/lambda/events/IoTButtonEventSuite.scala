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

import io.circe.Json
import io.circe.literal._
import munit.FunSuite

class IoTButtonEventSuite extends FunSuite {
  import IoTButtonEventSuite._

  test("decoder SINGLE click") {
    assertEquals(singleClickEvent.as[IoTButtonEvent].toTry.get, decodedSingle)
  }

  test("decoder LONG click") {
    assertEquals(longClickEvent.as[IoTButtonEvent].toTry.get, decodedLong)
  }
}

object IoTButtonEventSuite {

  val decodedSingle: IoTButtonEvent =
    IoTButtonEvent(
      clickType = IoTButtonEvent.ClickType.Single,
      serialNumber = "G030PM00000000000000",
      batteryVoltage = "1584mV"
    )

  val decodedLong: IoTButtonEvent =
    IoTButtonEvent(
      clickType = IoTButtonEvent.ClickType.Long,
      serialNumber = "G030PM00000000000000",
      batteryVoltage = "1584mV"
    )

  // AWS IoT Button payload: https://docs.aws.amazon.com/lambda/latest/dg/services-iot.html
  val singleClickEvent: Json = json"""
    {
      "clickType": "SINGLE",
      "serialNumber": "G030PM00000000000000",
      "batteryVoltage": "1584mV"
    }
  """

  val longClickEvent: Json = json"""
    {
      "clickType": "LONG",
      "serialNumber": "G030PM00000000000000",
      "batteryVoltage": "1584mV"
    }
  """
}
