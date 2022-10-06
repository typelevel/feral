package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseResult}
import org.typelevel.ci._
import cats.parse.{Parser0, Rfc5234}

class `Lambda-Runtime-Invoked-Function-Arn`(val value: String)

object `Lambda-Runtime-Invoked-Function-Arn`{

  val name: String = "Lambda-Runtime-Invoked-Function-Arn"

  def parse(s: String): ParseResult[`Lambda-Runtime-Invoked-Function-Arn`] =
    ParseResult.success(new `Lambda-Runtime-Invoked-Function-Arn`(s))

  implicit val headerInstance: Header[`Lambda-Runtime-Invoked-Function-Arn`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)
}