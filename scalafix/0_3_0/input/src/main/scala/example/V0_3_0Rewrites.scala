/* rule=V0_3_0Rewrites */

package example

// format: off
import cats.effect.Concurrent
import feral.lambda.LambdaEnv
import feral.lambda.ApiGatewayProxyLambdaEnv
import feral.lambda.DynamoDbStreamLambdaEnv
import feral.lambda.S3BatchLambdaEnv
import feral.lambda.SnsLambdaEnv
import feral.lambda.SqsLambdaEnv
import feral.lambda.events.APIGatewayProxyRequestEvent
import feral.lambda.events.APIGatewayProxyResponseEvent
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import feral.lambda.http4s.ApiGatewayProxyHandler
import org.http4s.HttpApp
// format: on

class Foo[F[_], E] {

  def bar(implicit env: LambdaEnv[F, E]): Unit = ???

}

object Handlers {
  def handler1[F[_]: Concurrent](
      implicit env: ApiGatewayProxyLambdaEnv[F]
  ): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandler.httpApp(HttpApp.notFound)
  def handler2[F[_]](implicit env: DynamoDbStreamLambdaEnv[F]): Unit = ???
  def handler3[F[_]](implicit env: S3BatchLambdaEnv[F]): Unit = ???
  def handler4[F[_]](implicit env: SnsLambdaEnv[F]): Unit = ???
  def handler5[F[_]](implicit env: SqsLambdaEnv[F]): Unit = ???
  def handler6(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = ???
}
