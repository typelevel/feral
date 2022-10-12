package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseFailure, ParseResult}
import org.typelevel.ci._
import cats.parse.{Parser0, Rfc5234}

class `Lambda-Runtime-Deadline-Ms`(val value: Long)

object `Lambda-Runtime-Deadline-Ms` {

  def apply(value: Long) = new `Lambda-Runtime-Deadline-Ms`(value)

  val name: String = "Lambda-Runtime-Deadline-Ms"

  def parse(s: String): ParseResult[`Lambda-Runtime-Deadline-Ms`] =
    s
    .toLongOption
    .toRight(new ParseFailure(s, "Unable to parse to millisecond value"))
    .map(new `Lambda-Runtime-Deadline-Ms`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Deadline-Ms`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)

}
