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

import cats.data.OptionT
import cats.effect.IO
import cats.effect.kernel.Resource
import com.amazonaws.services.lambda.{runtime => lambdaRuntime}
import io.circe
import io.circe.Printer
import io.circe.syntax._

import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

private[lambda] abstract class IOLambdaPlatform[Event, Result]
    extends lambdaRuntime.RequestStreamHandler { this: IOLambda[Event, Result] =>

  final def handleRequest(
      input: InputStream,
      output: OutputStream,
      context: lambdaRuntime.Context): Unit =
    Resource
      .eval {
        for {
          lambda <- setupMemo
          event <- fs2
            .io
            .readInputStream(IO.pure(input), 8192, closeAfterUse = false)
            .through(circe.fs2.byteStreamParser)
            .through(circe.fs2.decoder[IO, Event])
            .head
            .compile
            .lastOrError
          context <- IO(Context.fromJava[IO](context))
          _ <- OptionT(lambda(event, context)).foreachF { result =>
            Resource.fromAutoCloseable(IO(new OutputStreamWriter(output))).use { writer =>
              IO(Printer.noSpaces.unsafePrintToAppendable(result.asJson, writer))
            }
          }
        } yield ()
      }
      .onFinalize(IO(input.close()))
      .onFinalize(IO(output.close()))
      .use_
      .unsafeRunSync()(runtime)

}
