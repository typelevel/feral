package example

// format: off
import cats.effect.Concurrent
import feral.lambda.events.ApiGatewayProxyStructuredResultV2
import org.http4s.HttpApp
import feral.lambda.{ ApiGatewayProxyInvocationV2, DynamoDbStreamInvocation, Invocation, S3BatchInvocation, SnsInvocation, SqsInvocation }
import feral.lambda.events.{ ApiGatewayProxyEvent, ApiGatewayProxyResult }
import feral.lambda.http4s.ApiGatewayProxyHandlerV2
// format: on

class Foo[F[_], E] {

  def bar(implicit env: Invocation[F, E]): Unit = ???

}

object Handlers {
  def handler1[F[_]: Concurrent](
      implicit env: ApiGatewayProxyInvocationV2[F]
  ): F[Option[ApiGatewayProxyStructuredResultV2]] =
    ApiGatewayProxyHandlerV2.apply(HttpApp.notFound)
  def handler2[F[_]](implicit env: DynamoDbStreamInvocation[F]): Unit = ???
  def handler3[F[_]](implicit env: S3BatchInvocation[F]): Unit = ???
  def handler4[F[_]](implicit env: SnsInvocation[F]): Unit = ???
  def handler5[F[_]](implicit env: SqsInvocation[F]): Unit = ???
  def handler6(event: ApiGatewayProxyEvent): ApiGatewayProxyResult = ???
}
