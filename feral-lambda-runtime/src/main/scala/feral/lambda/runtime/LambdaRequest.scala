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
    // Still need to decide how to handle failed request or invalid header values
    def fromResponse[F[_]](response: Response[F])(implicit F: ApplicativeError[F, Throwable]): F[LambdaRequest] = {
        F.pure(LambdaRequest(???))
        //Unsure on best way to unpack the response headers into LambdaRequest case class
    }

}