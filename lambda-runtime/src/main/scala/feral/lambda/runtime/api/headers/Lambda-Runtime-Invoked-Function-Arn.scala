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
 * The ARN requested. This can be different in each invoke that executes the same version.
 */
private[runtime] final class `Lambda-Runtime-Invoked-Function-Arn`(val value: String)

private[runtime] object `Lambda-Runtime-Invoked-Function-Arn` {

  def apply(value: String) = new `Lambda-Runtime-Invoked-Function-Arn`(value)

  final val name = ci"Lambda-Runtime-Invoked-Function-Arn"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Invoked-Function-Arn`] =
    ParseResult.success(`Lambda-Runtime-Invoked-Function-Arn`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Invoked-Function-Arn`, Header.Single] =
    Header.createRendered(name, _.value, parse)
}
