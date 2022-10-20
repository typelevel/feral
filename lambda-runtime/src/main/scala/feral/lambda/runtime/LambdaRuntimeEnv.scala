package feral.lambda.runtime

import cats.MonadThrow
import cats.effect.std.Env
import cats.syntax.all._

trait LambdaRuntimeEnv[F[_]] {
  def lambdaFunctionName: F[String]
  def lambdaFunctionMemorySize: F[Int]
  def lambdaFunctionVersion: F[String]
  def lambdaLogGroupName: F[String]
  def lambdaLogStreamName: F[String]
  def lambdaRuntimeApi: F[String]
}

object LambdaRuntimeEnv {
  private[runtime] final val AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
  private[runtime] final val AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE"
  private[runtime] final val AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION"
  private[runtime] final val AWS_LAMBDA_LOG_GROUP_NAME = "AWS_LAMBDA_LOG_GROUP_NAME"
  private[runtime] final val AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME"
  private[runtime] final val AWS_LAMBDA_RUNTIME_API = "AWS_LAMBDA_RUNTIME_API"

  def apply[F[_]](implicit lre: LambdaRuntimeEnv[F]): LambdaRuntimeEnv[F] = lre

  implicit def forEnv[F[_]: MonadThrow](implicit env: Env[F]): LambdaRuntimeEnv[F] =
    new LambdaRuntimeEnv[F] {

      override def lambdaFunctionName: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_NAME)

      override def lambdaFunctionMemorySize: F[Int] =
        getOrRaise(AWS_LAMBDA_FUNCTION_MEMORY_SIZE).flatMap(value =>
          value.toIntOption.liftTo(new NumberFormatException(value)))

      override def lambdaFunctionVersion: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_VERSION)

      override def lambdaLogGroupName: F[String] = getOrRaise(AWS_LAMBDA_LOG_GROUP_NAME)

      override def lambdaLogStreamName: F[String] = getOrRaise(AWS_LAMBDA_LOG_STREAM_NAME)

      override def lambdaRuntimeApi: F[String] = getOrRaise(AWS_LAMBDA_RUNTIME_API)

      private[runtime] def getOrRaise(envName: String): F[String] =
        env.get(envName).flatMap(_.liftTo(new NoSuchElementException(envName)))
    }
}
