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

  type ApiGatewayProxyLambdaEnv[F[_]] = LambdaEnv[F, ApiGatewayProxyEventV2]
  type DynamoDbStreamLambdaEnv[F[_]] = LambdaEnv[F, DynamoDbStreamEvent]
  type KinesisStreamLambdaEnv[F[_]] = LambdaEnv[F, KinesisStreamEvent]
  type S3BatchLambdaEnv[F[_]] = LambdaEnv[F, S3BatchEvent]
  type SnsLambdaEnv[F[_]] = LambdaEnv[F, SnsEvent]
  type SqsLambdaEnv[F[_]] = LambdaEnv[F, SqsEvent]
}
