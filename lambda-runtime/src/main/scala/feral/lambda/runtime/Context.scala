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

package feral.lambda.runtime

import _root_.feral.lambda.{Context => IContext}
import cats.effect.Temporal
import cats.syntax.functor._

import api.LambdaRequest

private[runtime] object Context {
  def from[F[_]](request: LambdaRequest, settings: LambdaSettings)(
      implicit F: Temporal[F]): IContext[F] =
    IContext[F](
      settings.functionName,
      settings.functionVersion,
      request.invokedFunctionArn,
      settings.functionMemorySize,
      request.id,
      request.traceId,
      settings.logGroupName,
      settings.logStreamName,
      request.identity,
      request.clientContext,
      F.realTime.map(curTime => request.deadlineTime - curTime)
    )
}
