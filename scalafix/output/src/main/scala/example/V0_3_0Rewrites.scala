package example

import feral.lambda.{ ApiGatewayProxyV2Invocation, DynamoDbStreamInvocation, Invocation, S3BatchInvocation, SnsInvocation, SqsInvocation }
import feral.lambda.events.{ ApiGatewayProxyEvent, ApiGatewayProxyResult }

class Foo[F[_], E] {

  def bar(implicit env: Invocation[F, E]): Unit = ???

}

object Handlers {
  def handler1[F[_]](implicit env: ApiGatewayProxyV2Invocation[F]): Unit = ???
  def handler2[F[_]](implicit env: DynamoDbStreamInvocation[F]): Unit = ???
  def handler3[F[_]](implicit env: S3BatchInvocation[F]): Unit = ???
  def handler4[F[_]](implicit env: SnsInvocation[F]): Unit = ???
  def handler5[F[_]](implicit env: SqsInvocation[F]): Unit = ???
  def handler6(event: ApiGatewayProxyEvent): ApiGatewayProxyResult = ???
}
