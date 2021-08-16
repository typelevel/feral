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
import cats.syntax.all._
import com.amazonaws.services.lambda.{runtime => lambdaRuntime}
import io.circe.jawn._

import java.io.InputStream
import java.io.OutputStream

private[lambda] abstract class IOLambdaPlatform[Setup, Event, Result]
    extends lambdaRuntime.RequestStreamHandler { this: IOLambda[Setup, Event, Result] =>

  final def handleRequest(
      input: InputStream,
      output: OutputStream,
      context: lambdaRuntime.Context): Unit =
    Resource
      .eval {
        for {
          setup <- setupMemo
          event <- IO(input.readAllBytes()).map(decodeByteArray[Event]).rethrow
          context <- IO(Context.fromJava(context))
          _ <- OptionT(apply(event, context, setup)).foldF(IO.unit) { result =>
            // TODO can circe write directly to output?
            IO(output.write(encoder(result).noSpaces.getBytes()))
          }
        } yield ()
      }
      .onFinalize(IO(input.close()))
      .onFinalize(IO(output.close()))
      .use_
      .unsafeRunSync()

}
