package feral.lambda.runtime

import cats.effect.kernel.Sync

object LambdaEnvironmentVariables {
  val HANDLER = "_HANDLER"
  val AWS_REGION = "AWS_REGION"
  val AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV"
  val AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
  val AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE".asInstanceOf[Int]
  val AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION"
  val AWS_LAMBDA_LOG_GROUP_NAME = "AWS_LAMBDA_LOG_GROUP_NAME"
  val AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME"
  val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"
  val AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY"
  val AWS_LAMBDA_RUNTIME_API = "AWS_LAMBDA_RUNTIME_API"
  val LAMBDA_TASK_ROOT = "LAMBDA_TASK_ROOT"
  val LAMBDA_RUNTIME_DIR = "LAMBDA_RUNTIME_DIR"
  val TC = "TZ"

  def extractEnvVar[F[_]](envVar: String)(implicit F: Sync[F]): F[Option[String]] = 
    F.delay(sys.env.get(envVar)) // sys.env okay to use in native
}
