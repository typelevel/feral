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
import feral.lambda.runtime.headers._
final class LambdaRequest(
     val deadlineTimeInMs: FiniteDuration,
     val id: String,
     val invokedFunctionArn: String,
     val identity: Option[CognitoIdentity],
     val clientContext: Option[ClientContext],
     val body: Json
    )

object LambdaRequest {
    def fromResponse[F[_]](response: Response[F])(implicit F: Concurrent[F]): F[LambdaRequest] = {
        implicit val jsonDecoder: EntityDecoder[F, Json] = jsonDecoderIncremental
        for {
            id <- response.headers.get[`Lambda-Runtime-Aws-Request-Id`].liftTo(new NoSuchElementException(`Lambda-Runtime-Aws-Request-Id`.name))
            invokedFunctionArn <- response.headers.get[`Lambda-Runtime-Invoked-Function-Arn`].liftTo(new NoSuchElementException(`Lambda-Runtime-Invoked-Function-Arn`.name))
            deadlineTimeInMs <- response.headers.get[`Lambda-Runtime-Deadline-Ms`].liftTo(new NoSuchElementException(`Lambda-Runtime-Deadline-Ms`.name))
            identity <- response.headers.get[`Lambda-Runtime-Client-Identity`].pure
            clientContext <- response.headers.get[`Lambda-Runtime-Client-Context`].pure
            body <- response.as[Json]
        } yield {
            new LambdaRequest(
                FiniteDuration.apply(deadlineTimeInMs.value, TimeUnit.MILLISECONDS),
                id.value,
                invokedFunctionArn.value,
                identity.map(_.value),
                clientContext.map(_.value),
                body
            )
        }
    }
}

