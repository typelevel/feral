package feral.lambda.runtime.headers

import org.http4s._
import org.typelevel.ci._
import feral.lambda._
import io.circe.{Decoder, Encoder, JsonObject}
import io.circe.syntax.EncoderOps
import io.circe.jawn._
import cats.syntax.all._

final class `Lambda-Runtime-Client-Context`(val value: ClientContext)

object `Lambda-Runtime-Client-Context` {

  def apply(value: ClientContext) = new `Lambda-Runtime-Client-Context`(value)

  final val name: String = "Lambda-Runtime-Client-Context"

  private[headers] def parser(s: String): ParseResult[`Lambda-Runtime-Client-Context`] = (for {
    parsedJson <- parse(s)
    clientContext <- parsedJson.as[ClientContext]
  } yield `Lambda-Runtime-Client-Context`(clientContext))
    .leftMap(_ => new ParseFailure(s, "Unable to parse client identity header"))

  implicit val headerInstance: Header[`Lambda-Runtime-Client-Context`, Header.Single] =
    Header.create(CIString(name), _.value.asJson.toString, parser)

  implicit val clientContextClientDecoder: Decoder[ClientContextClient] = Decoder.forProduct5("client_id", "app_title", "app_version_name", "app_version_code", "app_package_name")(
    (clientId: String, appTitle: String, appVersionName: String, appVersionCode: String, appPackageName: String) =>
      new ClientContextClient(clientId, appTitle, appVersionName, appVersionCode, appPackageName)
  )

  implicit val clientContextEnvDecoder: Decoder[ClientContextEnv] = Decoder.forProduct5("platform", "model", "make", "platform_version", "locale")(
    (platform: String, model: String, make: String, platformVersion: String, locale: String) =>
      new ClientContextEnv(platformVersion, platform, make, model, locale)
  )

  implicit val clientContextDecoder: Decoder[ClientContext] = Decoder.forProduct3("client", "custom", "env")(
    (client: ClientContextClient, custom: JsonObject, env: ClientContextEnv) =>
      new ClientContext(client, env, custom)
  )

  implicit val clientContextClientEncoder: Encoder[ClientContextClient] =
    Encoder.forProduct5("client_id", "app_title", "app_version_name", "app_version_code", "app_package_name")(c =>
      (c.installationId, c.appTitle, c.appVersionName, c.appVersionCode, c.appPackageName)
    )

  implicit val clientContextEnvEncoder: Encoder[ClientContextEnv] =
    Encoder.forProduct5("platform", "model", "make", "platform_version", "locale")(c =>
      (c.platformVersion, c.platform, c.make, c.model, c.locale)
    )

  implicit val clientContextEncoder: Encoder[ClientContext] =
    Encoder.forProduct3("client", "custom", "env")(c =>
      (c.client, c.custom, c.env)
    )
}
