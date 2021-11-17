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

import cats.data._
import cats.effect.std.Random
import cats.effect.{Trace => _, _}
import feral.lambda._
import feral.lambda.tracing.TracedLambda.evalKernel
import natchez._
import natchez.noop.NoopSpan
import natchez.xray.{XRay, XRayEnvironment}

object XRayTracedLambda {
  def apply[F[_] : Async, Event, Result](installer: Resource[Kleisli[F, Span[F], *], Lambda[Kleisli[F, Span[F], *], Event, Result]])
                                        (implicit LT: LiftTrace[F, Kleisli[F, Span[F], *]]): Resource[F, Lambda[F, Event, Result]] =
    installer
      .mapK(Kleisli.applyK(new NoopSpan[F]()))
      .flatMap(XRayTracedLambda.usingEnvironment[F, Kleisli[F, Span[F], *], Event, Result](_))

  def usingEnvironment[F[_] : Async, G[_], Event, Result](lambda: Lambda[G, Event, Result])
                                                         (implicit LT: LiftTrace[F, G]): Resource[F, Lambda[F, Event, Result]] =
    XRayTracedLambda(evalKernel[Event](XRayEnvironment[F].kernelFromEnvironment))(lambda)

  def apply[F[_] : Async, G[_], Event, Result](extractKernel: Kleisli[F, (Event, Context[F]), Kernel])
                                              (lambda: Lambda[G, Event, Result])
                                              (implicit LT: LiftTrace[F, G]): Resource[F, Lambda[F, Event, Result]] = {
    Resource.eval(Random.scalaUtilRandom[F]).flatMap { implicit random =>
      Resource
        .eval(XRayEnvironment[F].daemonAddress)
        .flatMap {
          case Some(addr) => XRay.entryPoint[F](addr)
          case None => XRay.entryPoint[F]()
        }
        .map(TracedLambda(_, extractKernel)(lambda))
    }
  }
}
