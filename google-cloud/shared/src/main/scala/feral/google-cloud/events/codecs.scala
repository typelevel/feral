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

import io.circe.Decoder
import io.circe.DecodingFailure
import scodec.bits.ByteVector

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.util.Try

private object codecs {
  implicit def decodeInstantFromMillis: Decoder[Instant] =
    Decoder.decodeLong.emapTry { millis => Try(Instant.ofEpochMilli(millis)) }

  implicit def decodeDateTimetoInstant: Decoder[Option[Instant]] =
    Decoder.instance[Option[Instant]] { c =>
      c.as[String] match {
        case Right("") => Right(None)
        case Right(str) => {
          Try {
            val parsed_date_time =
              java.time.ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            parsed_date_time.toInstant()
          }.toEither.left.map(e => DecodingFailure(e.getMessage(), c.history)).map(Some(_))
        }
        case Left(_) => Right(None)
      }

    }

  implicit def decodeInstant: Decoder[Instant] =
    Decoder.decodeString.emapTry { str => Try(Instant.parse(str)) }

  implicit def decodeByteVector: Decoder[ByteVector] =
    Decoder.decodeString.emap { str =>
      ByteVector.fromBase64(str).toRight(s"Invalid base64 string")
    }

}
