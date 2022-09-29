package feral.lambda.runtime

import cats.{ApplicativeError, Functor}
import cats.effect.kernel.Sync
import cats.effect.std.Env
import cats.syntax.all._

trait LambdaRuntimeEnv[F[_]] {
  def handler: F[String]
  def region: F[String]
  def executionEnv: F[String]
  def lambdaFunctionName: F[String]
  def lambdaFunctionMemorySize: F[Int]
  def lambdaFunctionVersion: F[String]
  def lambdaLogGroupName: F[String]
  def lambdaLogStreamName: F[String]
  def accessKeyId: F[String]
  def secretAccessKey: F[String]
  def lambdaRuntimeApi: F[String]
  def lambdaTaskRoot: F[String]
  def lambdaRuntimeDir: F[String]
  def timezone: F[String]
}

object LambdaRuntimeEnv {
  final val HANDLER = "_HANDLER"
  final val AWS_REGION = "AWS_REGION"
  final val AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV"
  final val AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
  final val AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE"
  final val AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION"
  final val AWS_LAMBDA_LOG_GROUP_NAME = "AWS_LAMBDA_LOG_GROUP_NAME"
  final val AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME"
  final val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"
  final val AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY"
  final val AWS_LAMBDA_RUNTIME_API = "AWS_LAMBDA_RUNTIME_API"
  final val LAMBDA_TASK_ROOT = "LAMBDA_TASK_ROOT"
  final val LAMBDA_RUNTIME_DIR = "LAMBDA_RUNTIME_DIR"
  final val TZ = "TZ"

  def apply[F[_]: Functor](env: Env[F]): LambdaRuntimeEnv[F] = new LambdaRuntimeEnv[F] { // should be error effect?
    override def handler: F[String] = env.get(HANDLER).map(_.get)

    override def region: F[String] = env.get(AWS_REGION).map(_.get)

    override def executionEnv: F[String] = env.get(AWS_EXECUTION_ENV).map(_.get)

    override def lambdaFunctionName: F[String] = env.get(AWS_LAMBDA_FUNCTION_NAME).map(_.get)

    override def lambdaFunctionMemorySize: F[Int] = env.get(AWS_LAMBDA_FUNCTION_MEMORY_SIZE).map(_.get)

    override def lambdaFunctionVersion: F[String] = env.get(AWS_LAMBDA_FUNCTION_VERSION).map(_.get)

    override def lambdaLogGroupName: F[String] = env.get(AWS_LAMBDA_LOG_GROUP_NAME).map(_.get)

    override def lambdaLogStreamName: F[String] = env.get(AWS_LAMBDA_LOG_STREAM_NAME).map(_.get)

    override def accessKeyId: F[String] = env.get(AWS_ACCESS_KEY_ID).map(_.get)

    override def secretAccessKey: F[String] = env.get(AWS_SECRET_ACCESS_KEY).map(_.get)

    override def lambdaRuntimeApi: F[String] = env.get(AWS_LAMBDA_RUNTIME_API).map(_.get)

    override def lambdaTaskRoot: F[String] = env.get(LAMBDA_TASK_ROOT).map(_.get)

    override def lambdaRuntimeDir: F[String] = env.get(LAMBDA_RUNTIME_DIR).map(_.get)

    override def timezone: F[String] = env.get(TZ).map(_.get)
  }
}
