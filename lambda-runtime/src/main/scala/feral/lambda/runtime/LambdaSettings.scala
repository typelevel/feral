package feral.lambda.runtime

import cats.MonadThrow
import cats.syntax.all._

private[runtime] final class LambdaSettings(
    val functionName: String,
    val functionVersion: String,
    val functionMemorySize: Int,
    val logGroupName: String,
    val logStreamName: String
)

private[runtime] object LambdaSettings {

  def apply(
      functionName: String,
      functionVersion: String,
      functionMemorySize: Int,
      logGroupName: String,
      logStreamName: String
  ): LambdaSettings =
    new LambdaSettings(
      functionName,
      functionVersion,
      functionMemorySize,
      logGroupName,
      logStreamName
    )

  def fromLambdaEnvironment[F[_]](
      implicit F: MonadThrow[F],
      env: LambdaRuntimeEnv[F]): F[LambdaSettings] =
    (
      env.lambdaFunctionName,
      env.lambdaFunctionVersion,
      env.lambdaFunctionMemorySize,
      env.lambdaLogGroupName,
      env.lambdaLogStreamName
    ).mapN(LambdaSettings.apply)
}
