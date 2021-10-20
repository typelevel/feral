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

import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.kernel.Deferred
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.unsafe.IORuntime
import cats.syntax.all._

import scala.concurrent.Future

trait Feral[F[_]] {
  implicit protected def F: Async[F]

  protected def dispatcher: Dispatcher[F]

  protected type Setup
  protected def setup: Resource[F, Setup] = Resource.pure(null.asInstanceOf[Setup])

  final lazy val setupMemo: F[Setup] = {
    val deferred = Deferred.unsafe[F, Either[Throwable, Setup]]
    dispatcher.unsafeRunAndForget {
      setup.attempt.allocated.flatTap {
        case (setup, _) =>
          deferred.complete(setup)
      }
    }
    deferred.get.rethrow
  }

}

trait IOFeral extends Feral[IO] {

  protected def runtime: IORuntime = IORuntime.global

  implicit protected final def F = IO.asyncForIO

  protected final def dispatcher = new Dispatcher[IO] {
    override def unsafeToFutureCancelable[A](fa: IO[A]): (Future[A], () => Future[Unit]) =
      fa.unsafeToFutureCancelable()(runtime)
  }
}
