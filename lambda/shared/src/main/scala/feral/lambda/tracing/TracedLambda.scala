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

package feral.lambda.tracing

import cats._
import cats.data.Kleisli
import cats.effect.{Trace => _, _}
import feral.lambda._
import natchez._

object TracedLambda {
  def evalKernel[Event] = new PartiallyAppliedEvalKernel[Event]()

  def apply[F[_]: MonadCancelThrow, G[_], Event, Result](
      entryPoint: EntryPoint[F],
      fk: F ~> G,
      extractKernel: Kleisli[F, (Event, Context[F]), Kernel])(lambda: Lambda[G, Event, Result])(
      implicit LT: LiftTrace[F, G]): Lambda[F, Event, Result] =
    (event, context) =>
      Resource
        .eval(extractKernel((event, context)))
        .flatMap { kernel => entryPoint.continueOrElseRoot(context.functionName, kernel) }
        .use { span => LiftTrace[F, G].lift(span, lambda(event, context.mapK(fk))) }
}

class PartiallyAppliedEvalKernel[Event](private val dummy: Unit = ()) extends AnyVal {
  def apply[F[_]](fa: F[Kernel]): Kleisli[F, (Event, Context[F]), Kernel] = Kleisli.liftF(fa)
}
