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

import feral.lambda.events._
import io.circe.Encoder

import scala.annotation.nowarn

package object lambda {

  /**
   * Alias for `Nothing` which works better with type inference. Inspired by fs2, but inlined
   * here to avoid pulling in an otherwise-unnecessary dependency.
   */
  type INothing <: Nothing

  /**
   * This can't actually be used. It's here because `IOLambda` demands an Encoder for its result
   * type, which should be `Nothing` when no output is desired. Userland code will return an
   * `Option[Nothing]` which is only inhabited by `None`, and the encoder is only used when the
   * userland code returns `Some`.
   */
  @nowarn("msg=dead code following this construct")
  implicit val nothingEncoder: Encoder[INothing] = identity(_)

  type ApiGatewayProxyInvocation[F[_]] = Invocation[F, ApiGatewayProxyEvent]
  type ApiGatewayProxyV2Invocation[F[_]] = Invocation[F, ApiGatewayProxyEventV2]
  type DynamoDbStreamInvocation[F[_]] = Invocation[F, DynamoDbStreamEvent]
  type S3Invocation[F[_]] = Invocation[F, S3Event]
  type S3BatchInvocation[F[_]] = Invocation[F, S3BatchEvent]
  type SnsInvocation[F[_]] = Invocation[F, SnsEvent]
  type SqsInvocation[F[_]] = Invocation[F, SqsEvent]

  @deprecated("Renamed to Invocation", "0.3.0")
  type LambdaEnv[F[_], Event] = Invocation[F, Event]
  @deprecated("Renamed to Invocation", "0.3.0")
  val LambdaEnv = Invocation

  @deprecated("Renamed to ApiGatewayProxyV2Invocation", "0.3.0")
  type ApiGatewayProxyLambdaEnv[F[_]] = ApiGatewayProxyV2Invocation[F]
  @deprecated("Renamed to DynamoDbStreamInvocation", "0.3.0")
  type DynamoDbStreamLambdaEnv[F[_]] = DynamoDbStreamInvocation[F]
  @deprecated(
    "Moved to kinesis4cats. See https://etspaceman.github.io/kinesis4cats/feral/getting-started.html.",
    since = "0.3.0")
  type KinesisStreamLambdaEnv[F[_]] = Invocation[F, KinesisStreamEvent]
  @deprecated("Renamed to S3BatchInvocation", "0.3.0")
  type S3BatchLambdaEnv[F[_]] = S3BatchInvocation[F]
  @deprecated("Renamed to SnsInvocation", "0.3.0")
  type SnsLambdaEnv[F[_]] = SnsInvocation[F]
  @deprecated("Renamed to SqsInvocation", "0.3.0")
  type SqsLambdaEnv[F[_]] = SqsInvocation[F]
}
