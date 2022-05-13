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

package feral.lambda

import io.circe.Decoder

import java.time.Instant
import scala.util.Try

package object events {

  implicit lazy val instantDecoder: Decoder[Instant] =
    Decoder.decodeBigDecimal.emapTry { millis =>
      def round(x: BigDecimal) = x.setScale(0, BigDecimal.RoundingMode.DOWN)
      Try {
        val seconds = round(millis / 1000).toLongExact
        val nanos = round(millis % 1000 * 1e6).toLongExact
        Instant.ofEpochSecond(seconds, nanos)
      }
    }

}
