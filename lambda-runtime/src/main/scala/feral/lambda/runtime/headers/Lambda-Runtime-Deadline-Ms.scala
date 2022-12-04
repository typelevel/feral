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

package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._

private[runtime] final class `Lambda-Runtime-Deadline-Ms`(val value: Long)

private[runtime] object `Lambda-Runtime-Deadline-Ms` {

  def apply(value: Long) = new `Lambda-Runtime-Deadline-Ms`(value)

  final val name = ci"Lambda-Runtime-Deadline-Ms"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Deadline-Ms`] =
    s.toLongOption
      .toRight(ParseFailure(s, "Unable to parse to millisecond value"))
      .map(`Lambda-Runtime-Deadline-Ms`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Deadline-Ms`, Header.Single] =
    Header.createRendered(name, _.value, parse)

}
