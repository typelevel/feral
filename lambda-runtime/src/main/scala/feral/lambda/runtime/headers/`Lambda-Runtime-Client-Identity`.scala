package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseFailure, ParseResult}
import org.typelevel.ci._
import feral.lambda.CognitoIdentity
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import io.circe._

class `Lambda-Runtime-Client-Identity`(val value: CognitoIdentity)

object `Lambda-Runtime-Client-Identity` {

  def apply(value: CognitoIdentity) = new `Lambda-Runtime-Client-Identity`(value)

  val name: String = "Lambda-Runtime-Client-Identity"

  def parse(s: String): ParseResult[`Lambda-Runtime-Client-Identity`] =
    s
    .asJson
    .as[CognitoIdentity]
    .toOption
    .toRight(new ParseFailure(s, "Unable to parse client identity header"))
    .map(new `Lambda-Runtime-Client-Identity`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Client-Identity`, Header.Single] =
    Header.create(CIString(name), _.value.asJson.toString, parse)

  implicit val cognitoIdentityDecoder: Decoder[CognitoIdentity] =
    Decoder.forProduct2("identity_id", "identity_pool_id")((identityId: String, poolId: String) =>
      new CognitoIdentity(identityId, poolId)
    )

  implicit val cognitoIdentityEncoder: Encoder[CognitoIdentity] =
    Encoder.forProduct2("identity_id", "identity_pool_id")(c =>
      (c.identityId, c.identityPoolId)
    )
}
