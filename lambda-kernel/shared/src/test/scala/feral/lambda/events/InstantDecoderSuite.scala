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
import munit.ScalaCheckSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import java.time.Instant

class InstantDecoderSuite extends ScalaCheckSuite {

  import codecs.decodeInstant

  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary(
    Gen.long.map(Instant.ofEpochMilli(_)))

  property("round-trip") {
    forAll { (instant: Instant) =>
      val decoded = Json.fromLong(instant.toEpochMilli()).as[Instant].toTry.get
      assertEquals(decoded, instant)
    }
  }

}
