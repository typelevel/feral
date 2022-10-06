package feral.lambda.runtime.headers

import org.http4s.{Header, Method, ParseFailure, ParseResult}
import org.typelevel.ci._
import cats.parse.{Parser0, Rfc5234}
import feral.lambda.runtime.headers.`Lambda-Runtime-Client-Context`.headerInstance
import feral.lambda.{ClientContext, ClientContextClient, ClientContextEnv, CognitoIdentity}
import io.circe.{Decoder, JsonObject}
import io.circe.syntax.EncoderOps

class `Lambda-Runtime-Client-Context`(val value: ClientContext)

object `Lambda-Runtime-Client-Context` {

  val name: String = "Lambda-Runtime-Client-Context"

  def parse(s: String): ParseResult[`Lambda-Runtime-Client-Context`] =
    s
    .asJson
    .as[ClientContext]
    .toOption
    .toRight(new ParseFailure(s, "Unable to parse client identity header"))
    .map(new `Lambda-Runtime-Client-Context`(_))

  implicit val headerInstance: Header[`Lambda-Runtime-Client-Context`, Header.Single] =
    Header.create(CIString(name), _.value.toString, parse)

  implicit val clientContextDecoder: Decoder[ClientContext] = Decoder.forProduct4("client", "custom", "env", "services")(
    (client: ClientContextClient, custom: JsonObject, env: ClientContextEnv, _: JsonObject) =>
      new ClientContext(client, env, custom)
  )

  implicit val clientContextClientDecoder: Decoder[ClientContextClient] = Decoder.forProduct5("client_id", "app_title", "app_version_name", "app_version_code", "app_package_name")(
    (clientId: String, appTitle: String, appVersionName: String, appVersionCode: String, appPackageName: String) =>
      new ClientContextClient(clientId, appTitle, appVersionName, appVersionCode, appPackageName)
  )

  implicit val clientContextEnvDecoder: Decoder[ClientContextEnv] = Decoder.forProduct5("platform", "model", "make", "platform_version", "locale")(
    (platform: String, model: String, make: String, platformVersion: String, locale: String) =>
      new ClientContextEnv(platformVersion, platform, make, model, locale)
  )

}
