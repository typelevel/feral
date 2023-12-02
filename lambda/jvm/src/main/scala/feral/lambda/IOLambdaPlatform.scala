/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feral.lambda

import cats.effect.IO
import cats.syntax.all._
import com.amazonaws.services.lambda.{runtime => lambdaRuntime}
import io.circe.Printer
import io.circe.jawn
import io.circe.syntax._

import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.channels.Channels
import scala.concurrent.Await
import scala.concurrent.duration.Duration

private[lambda] abstract class IOLambdaPlatform[Event, Result]
    extends lambdaRuntime.RequestStreamHandler { this: IOLambda[Event, Result] =>

  final def handleRequest(
      input: InputStream,
      output: OutputStream,
      runtimeContext: lambdaRuntime.Context): Unit = {
    val (dispatcher, lambda) =
      Await.result(setupMemo, Duration.Inf)

    dispatcher.unsafeRunSync {
      val event =
        IO(jawn.decodeChannel[Event](Channels.newChannel(input))).flatMap(IO.fromEither)

      val context = IO(Context.fromJava[IO](runtimeContext))

      (event, context).flatMapN(lambda(_, _)).flatMap {
        case Some(result) =>
          IO {
            val writer = new OutputStreamWriter(output)
            Printer.noSpaces.unsafePrintToAppendable(result.asJson, writer)
            writer.flush()
          }
        case None => IO.unit
      }
    }
  }

}
