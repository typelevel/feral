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
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.amazonaws.services.lambda.{runtime => lambdaRuntime}
import io.circe

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

private[lambda] abstract class LambdaPlatform[F[_], Event, Result]
    extends lambdaRuntime.RequestStreamHandler { this: Lambda[F, Event, Result] =>

  final def handleRequest(
      input: InputStream,
      output: OutputStream,
      context: lambdaRuntime.Context): Unit = dispatcher.unsafeRunSync {
    Resource
      .eval {
        for {
          setup <- setupMemo
          event <- fs2
            .io
            .readInputStream(input.pure, 8192, closeAfterUse = false)
            .through(circe.fs2.byteStreamParser)
            .through(circe.fs2.decoder[F, Event])
            .head
            .compile
            .lastOrError
          context <- F.delay(Context.fromJava(context))
          _ <- OptionT(apply(event, context, setup)).foldF(F.unit) { result =>
            // TODO can circe write directly to output?
            F.delay(output.write(encoder(result).noSpaces.getBytes(StandardCharsets.UTF_8)))
          }
        } yield ()
      }
      .onFinalize(F.delay(input.close()))
      .onFinalize(F.delay(output.close()))
      .use_
  }

}
