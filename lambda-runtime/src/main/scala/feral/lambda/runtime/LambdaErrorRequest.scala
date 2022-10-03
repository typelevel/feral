package feral.lambda.runtime

import io.circe.Encoder

final class LambdaErrorRequest(val errorMessage: String, val errorType: String, val stackTrace: List[String])

object LambdaErrorRequest {
  def apply(errorMessage: String, errorType: String, stackTrace: List[String]) =
    new LambdaErrorRequest(errorMessage, errorType, stackTrace)

  implicit val encoder: Encoder[LambdaErrorRequest] =
    Encoder.forProduct3("errorMessage", "errorType", "stackTrace")(e =>
      (e.errorMessage, e.errorType, e.stackTrace))
}
