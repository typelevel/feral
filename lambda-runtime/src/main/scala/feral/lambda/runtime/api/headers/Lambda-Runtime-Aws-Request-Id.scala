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

package feral.lambda.runtime.api.headers

import org.http4s._
import org.typelevel.ci._

/**
 * AWS request ID associated with the request.
 */
private[runtime] final class `Lambda-Runtime-Aws-Request-Id`(val value: String)

private[runtime] object `Lambda-Runtime-Aws-Request-Id` {

  def apply(value: String) = new `Lambda-Runtime-Aws-Request-Id`(value)

  final val name = ci"Lambda-Runtime-Aws-Request-Id"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Aws-Request-Id`] =
    ParseResult.success(`Lambda-Runtime-Aws-Request-Id`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Aws-Request-Id`, Header.Single] =
    Header.createRendered(name, _.value, parse)
}
