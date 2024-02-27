package feral.lambda.otel4s

import feral.lambda.events.SqsEvent
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.SpanKind
import feral.lambda.events.DynamoDbStreamEvent
import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.S3BatchEvent

protected[lambda] trait EventAttributeSources {
  implicit def sqsEvent: EventSpanAttributes[SqsEvent] =
    new EventSpanAttributes[SqsEvent] {
      def contextCarrier(e: SqsEvent): Map[String, String] =
        Map.empty

      def spanKind: SpanKind = SpanKind.Consumer

      def attributes(e: SqsEvent): List[Attribute[_]] =
        SqsEventTraceAttributes()
    }

  implicit def dynamoDbStreamEvent: EventSpanAttributes[DynamoDbStreamEvent] = ???

  implicit def apiGatewayProxyEvent: EventSpanAttributes[ApiGatewayProxyEvent] = ???

  implicit def apiGatewayProxyEventV2: EventSpanAttributes[ApiGatewayProxyEventV2] = ???

  implicit def s3BatchEvent: EventSpanAttributes[S3BatchEvent] = ???
}
