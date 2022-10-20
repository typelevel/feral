package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._
import feral.lambda.CognitoIdentity
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import io.circe._
import io.circe.jawn._
import cats.syntax.all._

final class `Lambda-Runtime-Client-Identity`(val value: CognitoIdentity)

object `Lambda-Runtime-Client-Identity` {

  def apply(value: CognitoIdentity) = new `Lambda-Runtime-Client-Identity`(value)

  final val name: String = "Lambda-Runtime-Client-Identity"
  private[headers] def parser(s: String): ParseResult[`Lambda-Runtime-Client-Identity`] = (for {
    parsedJson <- parse(s)
    identity <- parsedJson.as[CognitoIdentity]
  } yield `Lambda-Runtime-Client-Identity`(identity))
    .leftMap(_ => new ParseFailure(s, "Unable to parse client identity header"))

  implicit val headerInstance: Header[`Lambda-Runtime-Client-Identity`, Header.Single] =
    Header.create(CIString(name), _.value.asJson.toString, parser)

  implicit val cognitoIdentityDecoder: Decoder[CognitoIdentity] =
    Decoder.forProduct2("identity_id", "identity_pool_id")((identityId: String, poolId: String) =>
      new CognitoIdentity(identityId, poolId)
    )

  implicit val cognitoIdentityEncoder: Encoder[CognitoIdentity] =
    Encoder.forProduct2("identity_id", "identity_pool_id")(c =>
      (c.identityId, c.identityPoolId)
    )
}
