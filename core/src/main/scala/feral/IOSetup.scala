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

package feral

import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.all._
import natchez.Span
import natchez.noop.NoopSpan

import scala.annotation.nowarn

private[feral] trait IOSetup {

  protected def runtime: IORuntime = IORuntime.global

  protected type Setup
  protected def setup: Resource[IO, Setup] = Resource.pure(null.asInstanceOf[Setup])

  @nowarn(value = "msg=parameter value name in method traceRootSpan is never used")
  protected def traceRootSpan(name: String): Resource[IO, Span[IO]] =
    Resource.pure(NoopSpan[IO]())

  private[feral] final lazy val setupMemo: IO[Setup] = {
    val deferred = Deferred.unsafe[IO, Either[Throwable, Setup]]
    setup
      .attempt
      .allocated
      .flatTap {
        case (setup, _) =>
          deferred.complete(setup)
      }
      .unsafeRunAndForget()(runtime)
    deferred.get.rethrow
  }

}
