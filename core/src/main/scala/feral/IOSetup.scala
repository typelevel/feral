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

import cats.data.OptionT
import cats.effect.IO
import cats.effect.kernel.{Deferred, Resource}
import cats.syntax.all._

private[feral] trait IOSetup[Context] {

  protected type Setup
  protected def setup(ctx: Context): Resource[IO, Setup] =
    Resource.pure(null.asInstanceOf[Setup])

  private[this] final lazy val deferred = Deferred.unsafe[IO, Either[Throwable, Setup]]

  private[feral] final def setupMemo(ctx: Context): IO[Setup] =
    OptionT(deferred.tryGet)
      .getOrElseF(
        setup(ctx).attempt.allocated.flatMap {
          case (setup, _) =>
            deferred.complete(setup).as(setup)
        }
      )
      .rethrow

}

private[feral] object IOSetup {
  private[feral] def apply[Context, Result](setupRes: Context => Resource[IO, Result])(
      ctx: Context): IO[Result] =
    new IOSetup[Context] {
      override protected type Setup = Result

      override protected def setup(ctx: Context): Resource[IO, Setup] = setupRes(ctx)
    }.setupMemo(ctx)
}
