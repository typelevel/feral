package feral.lambda.runtime

import io.circe.Encoder

final class LambdaErrorBody(val errorMessage: String, val errorType: String, val stackTrace: List[String])

object LambdaErrorBody {
  def apply(errorMessage: String, errorType: String, stackTrace: List[String]) =
    new LambdaErrorBody(errorMessage, errorType, stackTrace)

  implicit val encoder: Encoder[LambdaErrorBody] =
    Encoder.forProduct3("errorMessage", "errorType", "stackTrace")(e =>
      (e.errorMessage, e.errorType, e.stackTrace))
}
