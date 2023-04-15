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

import scala.concurrent.Future

import cats.effect.unsafe.IORuntime
import cats.effect.kernel.Resource
import cats.effect.IO
import cats.effect.std.Dispatcher
import cats.syntax.all._

private[feral] trait IOSetup {

  protected def runtime: IORuntime = IORuntime.global

  protected type Setup
  protected def setup: Resource[IO, Setup] = Resource.pure(null.asInstanceOf[Setup])

  private[feral] final lazy val setupMemo: Future[(Dispatcher[IO], Setup)] =
    (Dispatcher.parallel[IO](await = false), setup)
      .tupled
      .allocated
      .map(_._1) // drop unused finalizer
      .unsafeToFuture()(runtime)

}
