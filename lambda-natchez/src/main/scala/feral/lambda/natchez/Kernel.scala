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

import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.ApiGatewayProxyResultV2
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import natchez.Kernel

import scala.annotation.nowarn

trait KernelSource[Event] {
  def extract(event: Event): Kernel
}

object KernelSource extends KernelSourceLowPriority {
  @inline def apply[E](implicit ev: KernelSource[E]): ev.type = ev

  implicit val apiGatewayProxyEventV2KernelSource: KernelSource[ApiGatewayProxyEventV2] =
    e => Kernel(e.headers)
}

private[natchez] sealed class KernelSourceLowPriority {
  implicit def emptyKernelSource[E]: KernelSource[E] = _ => Kernel(Map.empty)
}

trait KernelSink[Result] {
  def seed(result: Result, kernel: Kernel): Result
}

object KernelSink {
  @inline def apply[R](implicit ev: KernelSink[R]): ev.type = ev

  implicit val apiGateProxyResultV2KernelSink: KernelSink[ApiGatewayProxyResultV2] = {
    case (result: ApiGatewayProxyStructuredResultV2, kernel) =>
      result.copy(headers = kernel.toHeaders ++ result.headers)
  }

  @nowarn("msg=dead code following this construct")
  implicit val nothingKernelSink: KernelSink[Nothing] = (result, _) => result
}
