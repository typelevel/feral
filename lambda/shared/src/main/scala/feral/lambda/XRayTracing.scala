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

/* TODO this file won't compile until these PRs are merged & published:
 * https://github.com/christiankjaer/natchez/pull/1
 * https://github.com/tpolecat/natchez/pull/427
 */

package feral.lambda

import cats.effect.std.Random
import cats.effect.{IO, Resource}
import feral.IOSetup
import natchez.xray.{XRay, XRayEnvironment}
import natchez.{EntryPoint, Span}

trait XRayTracing extends IOSetup {
  private def traceEntryPoint: Resource[IO, EntryPoint[IO]] =
    Resource.eval(Random.scalaUtilRandom[IO]).flatMap { implicit random =>
      Resource.eval(XRayEnvironment[IO].daemonAddress).flatMap {
        case Some(addr) => XRay.entryPoint[IO](addr)
        case None => XRay.entryPoint[IO]()
      }
  }

  override protected def traceRootSpan(name: String): Resource[IO, Span[IO]] =
    Resource.eval(XRayEnvironment[IO].kernelFromEnvironment).flatMap { kernel =>
      traceEntryPoint.flatMap(_.continueOrElseRoot(name, kernel))
    }
}
