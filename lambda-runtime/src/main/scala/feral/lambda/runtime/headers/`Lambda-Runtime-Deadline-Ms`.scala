package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._

final class `Lambda-Runtime-Deadline-Ms`(val value: Long)

object `Lambda-Runtime-Deadline-Ms` {

  def apply(value: Long) = new `Lambda-Runtime-Deadline-Ms`(value)

  final val name: String = "Lambda-Runtime-Deadline-Ms"

  private[headers] def parse(s: String): ParseResult[`Lambda-Runtime-Deadline-Ms`] =
    s
    .toLongOption
    .toRight(new ParseFailure(s, "Unable to parse to millisecond value"))
    .map(`Lambda-Runtime-Deadline-Ms`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Deadline-Ms`, Header.Single] =
    Header.createRendered(CIString(name), _.value, parse)

}
