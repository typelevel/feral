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

import cats.MonadThrow
import cats.effect.std.Env
import cats.syntax.all._
import org.http4s.Uri

trait LambdaRuntimeEnv[F[_]] {

  /**
   * The name of the function.
   */
  def lambdaFunctionName: F[String]

  /**
   * The amount of memory available to the function in MB.
   */
  def lambdaFunctionMemorySize: F[Int]

  /**
   * The version of the function being executed.
   */
  def lambdaFunctionVersion: F[String]

  /**
   * The name of the Amazon CloudWatch Logs group for the function.
   */
  def lambdaLogGroupName: F[String]

  /**
   * The name of the Amazon CloudWatch Logs stream for the function.
   */
  def lambdaLogStreamName: F[String]

  /**
   * The host and port of the runtime API.
   */
  def lambdaRuntimeApi: F[Uri]
}

object LambdaRuntimeEnv {
  private[runtime] final val AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
  private[runtime] final val AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE"
  private[runtime] final val AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION"
  private[runtime] final val AWS_LAMBDA_LOG_GROUP_NAME = "AWS_LAMBDA_LOG_GROUP_NAME"
  private[runtime] final val AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME"
  private[runtime] final val AWS_LAMBDA_RUNTIME_API = "AWS_LAMBDA_RUNTIME_API"

  def apply[F[_]](implicit lre: LambdaRuntimeEnv[F]): LambdaRuntimeEnv[F] = lre

  implicit def forEnv[F[_]: MonadThrow: Env]: LambdaRuntimeEnv[F] =
    new LambdaRuntimeEnv[F] {

      def lambdaFunctionName: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_NAME)

      def lambdaFunctionMemorySize: F[Int] =
        getOrRaise(AWS_LAMBDA_FUNCTION_MEMORY_SIZE).flatMap(value =>
          value.toIntOption.liftTo(new NumberFormatException(value)))

      def lambdaFunctionVersion: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_VERSION)

      def lambdaLogGroupName: F[String] = getOrRaise(AWS_LAMBDA_LOG_GROUP_NAME)

      def lambdaLogStreamName: F[String] = getOrRaise(AWS_LAMBDA_LOG_STREAM_NAME)

      def lambdaRuntimeApi: F[Uri] =
        getOrRaise(AWS_LAMBDA_RUNTIME_API).flatMap(host =>
          Uri.fromString(s"http://$host").liftTo[F])

      def getOrRaise(envName: String): F[String] =
        Env[F].get(envName).flatMap(_.liftTo(new NoSuchElementException(envName)))
    }
}
