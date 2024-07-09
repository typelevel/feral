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

import cats.effect.Async
import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Dispatcher
import cats.effect.syntax.all._
import cats.syntax.all._
import com.amazonaws.services.lambda.{runtime => lambdaRuntime}
import io.circe.Printer
import io.circe.jawn
import io.circe.syntax._

import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.channels.Channels
import scala.concurrent.duration._
import scala.util.control.NonFatal

private[lambda] abstract class IOLambdaPlatform[Event, Result]
    extends lambdaRuntime.RequestStreamHandler { this: IOLambda[Event, Result] =>

  private[this] val (dispatcher, handle) = {
    val handler = {
      val h =
        try this.handler
        catch { case ex if NonFatal(ex) => null }

      if (h ne null) {
        h.map(IO.pure(_))
      } else {
        val lambdaName = getClass().getSimpleName()
        val msg =
          s"""|There was an error initializing `$lambdaName` during startup.
              |Falling back to initialize-during-first-invocation strategy.
              |To fix, try replacing any `val`s in `$lambdaName` with `def`s.""".stripMargin
        System.err.println(msg)

        Async[Resource[IO, *]].defer(this.handler).memoize.map(_.allocated.map(_._1))
      }
    }

    Dispatcher
      .parallel[IO](await = false)
      .product(handler)
      .allocated
      .map(_._1) // drop unused finalizer
      .unsafeRunSync()(runtime)
  }

  final def handleRequest(
      input: InputStream,
      output: OutputStream,
      runtimeContext: lambdaRuntime.Context): Unit = {
    val event = jawn.decodeChannel[Event](Channels.newChannel(input)).fold(throw _, identity(_))
    val context = ContextPlatform.fromJava[IO](runtimeContext)
    dispatcher
      .unsafeRunTimed(
        for {
          f <- handle
          ctx <- context
          result <- f(Invocation.pure(event, ctx))
        } yield result,
        runtimeContext.getRemainingTimeInMillis().millis
      )
      .foreach { result =>
        val writer = new OutputStreamWriter(output)
        Printer.noSpaces.unsafePrintToAppendable(result.asJson, writer)
        writer.flush()
      }
  }

}
