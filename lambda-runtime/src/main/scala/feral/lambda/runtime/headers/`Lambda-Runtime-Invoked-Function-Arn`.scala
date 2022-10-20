package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._

final class `Lambda-Runtime-Invoked-Function-Arn`(val value: String)

object `Lambda-Runtime-Invoked-Function-Arn` {

  def apply(value: String) = new `Lambda-Runtime-Invoked-Function-Arn`(value)

  final val name: String = "Lambda-Runtime-Invoked-Function-Arn"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Invoked-Function-Arn`] =
    ParseResult.success(`Lambda-Runtime-Invoked-Function-Arn`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Invoked-Function-Arn`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)
}
