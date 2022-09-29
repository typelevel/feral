package feral.lambda.runtime

import cats.ApplicativeError
import io.circe._

import java.time.Instant
import org.http4s.Response

case class LambdaRequest(
    deadlineTimeInMs: Instant,
    id: String,
    invokedFunctionArn: String,
    body: Json
    )

object LambdaRequest {
    def fromResponse[F[_]](response: Response[F])(implicit F: ApplicativeError[F, Throwable]): F[LambdaRequest] = {
        ???
    }

}