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

// AWS IoT Button payload. DefinitelyTyped trigger/iot.d.ts models IoTEvent as string | number | T
// (no fixed structure). This model follows the documented AWS IoT Button payload:
// https://docs.aws.amazon.com/lambda/latest/dg/services-iot.html (IoT as trigger)
// clickType: SINGLE | DOUBLE | LONG, serialNumber, batteryVoltage (e.g. "1584mV")

sealed abstract class IoTButtonEvent {
  def clickType: IoTButtonEvent.ClickType
  def serialNumber: String
  def batteryVoltage: String
}

object IoTButtonEvent {

  sealed abstract class ClickType
  object ClickType {
    case object Single extends ClickType
    case object Double extends ClickType
    case object Long extends ClickType

    implicit val decoder: Decoder[ClickType] = Decoder.decodeString.emap {
      case "SINGLE" => Right(Single)
      case "DOUBLE" => Right(Double)
      case "LONG" => Right(Long)
      case other => Left(s"Unknown click type: $other")
    }
  }

  def apply(
      clickType: ClickType,
      serialNumber: String,
      batteryVoltage: String
  ): IoTButtonEvent =
    new Impl(clickType, serialNumber, batteryVoltage)

  implicit val decoder: Decoder[IoTButtonEvent] =
    Decoder.forProduct3("clickType", "serialNumber", "batteryVoltage")(
      IoTButtonEvent.apply
    )

  private final case class Impl(
      clickType: ClickType,
      serialNumber: String,
      batteryVoltage: String
  ) extends IoTButtonEvent {
    override def productPrefix = "IoTButtonEvent"
  }
}
