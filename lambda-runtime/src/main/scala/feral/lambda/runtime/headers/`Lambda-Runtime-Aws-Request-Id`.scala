package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseResult}
import org.typelevel.ci._
import cats.parse.{Parser0, Rfc5234}

class `Lambda-Runtime-Aws-Request-Id`(val value: String)

object `Lambda-Runtime-Aws-Request-Id` {

  def apply(value: String) = new `Lambda-Runtime-Aws-Request-Id`(value)

  val name: String = "Lambda-Runtime-Aws-Request-Id"

  def parse(s: String): ParseResult[`Lambda-Runtime-Aws-Request-Id`] =
    ParseResult.success(new `Lambda-Runtime-Aws-Request-Id`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Aws-Request-Id`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)
}
