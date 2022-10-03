package feral.lambda.runtime

import cats.{ApplicativeError, Functor, MonadThrow}
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
  private[this] final val HANDLER = "_HANDLER"
  private[this] final val AWS_REGION = "AWS_REGION"
  private[this] final val AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV"
  private[this] final val AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
  private[this] final val AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE"
  private[this] final val AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION"
  private[this] final val AWS_LAMBDA_LOG_GROUP_NAME = "AWS_LAMBDA_LOG_GROUP_NAME"
  private[this] final val AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME"
  private[this] final val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"
  private[this] final val AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY"
  private[this] final val AWS_LAMBDA_RUNTIME_API = "AWS_LAMBDA_RUNTIME_API"
  private[this] final val LAMBDA_TASK_ROOT = "LAMBDA_TASK_ROOT"
  private[this] final val LAMBDA_RUNTIME_DIR = "LAMBDA_RUNTIME_DIR"
  private[this] final val TZ = "TZ"

  def apply[F[_]](implicit lre: LambdaRuntimeEnv[F]): LambdaRuntimeEnv[F] = lre

  implicit def forEnv[F[_]: MonadThrow](implicit env: Env[F]): LambdaRuntimeEnv[F] = new LambdaRuntimeEnv[F] {
    override def handler: F[String] = getOrRaise(HANDLER)

    override def region: F[String] = getOrRaise(AWS_REGION)

    override def executionEnv: F[String] = getOrRaise(AWS_EXECUTION_ENV)

    override def lambdaFunctionName: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_NAME)

    override def lambdaFunctionMemorySize: F[Int] = getOrRaise(AWS_LAMBDA_FUNCTION_MEMORY_SIZE).map(_.toInt) // TODO better way?

    override def lambdaFunctionVersion: F[String] = getOrRaise(AWS_LAMBDA_FUNCTION_VERSION)

    override def lambdaLogGroupName: F[String] = getOrRaise(AWS_LAMBDA_LOG_GROUP_NAME)

    override def lambdaLogStreamName: F[String] = getOrRaise(AWS_LAMBDA_LOG_STREAM_NAME)

    override def accessKeyId: F[String] = getOrRaise(AWS_ACCESS_KEY_ID)

    override def secretAccessKey: F[String] = getOrRaise(AWS_SECRET_ACCESS_KEY)

    override def lambdaRuntimeApi: F[String] = getOrRaise(AWS_LAMBDA_RUNTIME_API)

    override def lambdaTaskRoot: F[String] = getOrRaise(LAMBDA_TASK_ROOT)

    override def lambdaRuntimeDir: F[String] = getOrRaise(LAMBDA_RUNTIME_DIR)

    override def timezone: F[String] = getOrRaise(TZ)

    private[this] def getOrRaise(envName: String): F[String] =
      env.get(envName).flatMap(_.liftTo(new NoSuchElementException(envName)))
  }
}
