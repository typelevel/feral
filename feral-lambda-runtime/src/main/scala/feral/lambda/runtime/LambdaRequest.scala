package feral.lambda.runtime

import io.circe._
import java.time.Instant

case class LambdaRequest(val deadlineTimeInMs: Instant, val id: String, val invokedFunctionArn: String, body: Json)

object LambdaRequest {
    implicit val decoder: Decoder[LambdaRequest] = 
        Decoder.forProduct4("deadlineTimeInMs", "id", "invokedFunctionArn", "body")(
            LambdaRequest.apply)
}



// final def apply(c: HCursor): Decoder.Result[LambdaRequest] = 
//             for {
//                 deadlineTimeInMs <- ???
//                 id <- ???
//                 invokedFunctionArn <- ???
//                 body <- ???
//             } yield {LambdaRequest(deadlineTimeInMs, id, invokedFunctionArn, body)}
