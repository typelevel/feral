package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseFailure, ParseResult}
import org.typelevel.ci._
import cats.parse.{Parser0, Rfc5234}
import feral.lambda.CognitoIdentity
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import org.http4s.util.{Renderer, Writer}
import io.circe._

class `Lambda-Runtime-Client-Identity`(val value: CognitoIdentity)

object `Lambda-Runtime-Client-Identity` {

  val name: String = "Lambda-Runtime-Client-Identity"

  def parse(s: String): ParseResult[`Lambda-Runtime-Client-Identity`] =
    s
    .asJson
    .as[CognitoIdentity]
    .toOption
    .toRight(new ParseFailure(s, "Unable to parse client identity header"))
    .map(new `Lambda-Runtime-Client-Identity`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Client-Identity`, Header.Single] =
    Header.create(CIString(name), _.value.toString, parse) //NOTE string representation of CognitoIdentity needed so that Lambda-Runtime-Client-Identity headers can be created. Since we are only parsing and not creating, is .toString fine?

  implicit val cognitoIdentityDecoder: Decoder[CognitoIdentity] =
    Decoder.forProduct2("identity_id", "identity_pool_id")((identityId: String, poolId: String) => // field names are just a guess for just now until I find the schema
      new CognitoIdentity(identityId, poolId))

}
