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

package feral.lambda.natchez

import cats.effect.kernel.MonadCancelThrow
import feral.lambda.Lambda
import natchez.EntryPoint
import natchez.Trace

object TracedLambda {
  def apply[F[_]: MonadCancelThrow, G[_], Event: HasKernel, Result](entryPoint: EntryPoint[F])(
      lambda: Trace[G] => Lambda[G, Event, Result])(
      implicit lift: LiftTrace[F, G]): Lambda[F, Event, Result] = { (event, context) =>
    val kernel = HasKernel[Event].extract(event)
    entryPoint.continueOrElseRoot(context.functionName, kernel).use { span =>
      lift.run(span)(lambda(_)(event, context.mapK(lift.liftK)))
    }
  }
}
