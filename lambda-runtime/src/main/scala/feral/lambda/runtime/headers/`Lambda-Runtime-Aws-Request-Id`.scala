package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._

final class `Lambda-Runtime-Aws-Request-Id`(val value: String)

object `Lambda-Runtime-Aws-Request-Id` {

  def apply(value: String) = new `Lambda-Runtime-Aws-Request-Id`(value)

  final val name: String = "Lambda-Runtime-Aws-Request-Id"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Aws-Request-Id`] =
    ParseResult.success(`Lambda-Runtime-Aws-Request-Id`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Aws-Request-Id`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)
}
