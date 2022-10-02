package feral.lambda.runtime

import cats.effect.kernel.Concurrent
import io.circe._
import cats.syntax.all._
import feral.lambda.{ClientContext, ClientContextClient, ClientContextEnv, CognitoIdentity}
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.{EntityDecoder, Headers, Response, headers}
import org.typelevel.ci.CIString
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

final class LambdaRequest(
     val deadlineTimeInMs: FiniteDuration,
     val id: String,
     val invokedFunctionArn: String,
     val identity: Option[CognitoIdentity],
     val clientContext: Option[ClientContext],
     val body: Json
    )

object LambdaRequest {
    private[this] final val requestIdHeader = "Lambda-Runtime-Aws-Request-id"
    private[this] final val invokedFunctionArnHeader = "Lambda-Runtime-Invoked-Function-Arn"
    private[this] final val deadlineTimeHeader = "Lambda-Runtime-Deadline-Ms"
    private[this] final val cognitoIdentityHeader = "Lambda-Runtime-Cognito-Identity"
    private[this] final val clientContextHeader = "Lambda-Runtime-Client-Context"
    def fromResponse[F[_]](response: Response[F])(implicit F: Concurrent[F]): F[LambdaRequest] = {
        implicit val jsonDecoder: EntityDecoder[F, Json] = jsonDecoderIncremental
        for { // TODO use custom header model for aws headers instead
            id <- F.fromOption(response.headers.get(CIString(requestIdHeader)), new NoSuchElementException(requestIdHeader)).map(_.head.value)
            invokedFunctionArn <- F.fromOption(response.headers.get(CIString(invokedFunctionArnHeader)), new NoSuchElementException(invokedFunctionArnHeader)).map(_.head.value)
            deadlineTimeInMs <- F.fromOption(response.headers.get(CIString(deadlineTimeHeader)), new NoSuchElementException(deadlineTimeHeader)).map(_.head.value.toLong)
            identity <- F.pure(response.headers.get(CIString(cognitoIdentityHeader)).flatMap(_.head.value.asJson.as[CognitoIdentity].toOption))
            clientContext <- F.pure(response.headers.get(CIString(clientContextHeader)).flatMap(_.head.value.asJson.as[ClientContext].toOption))
            body <- F.rethrow(jsonDecoder.decode(response, strict = false).value) // basically a reimplementation of response.as[Json] from dsl
        } yield {
            new LambdaRequest(FiniteDuration.apply(deadlineTimeInMs, TimeUnit.MILLISECONDS), id, invokedFunctionArn, identity, clientContext, body)
        }
    }

    implicit val cognitoIdentityDecoder: Decoder[CognitoIdentity] =
        Decoder.forProduct2("identity_id", "identity_pool_id")((identityId: String, poolId: String) => // field names are just a guess for just now until I find the schema
            new CognitoIdentity(identityId, poolId))

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