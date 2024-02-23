package feral.lambda.otel4s

import feral.lambda.events.SqsRecord
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.trace.attributes.SemanticAttributes

object SqsEventTraceAttributes {
  def apply(): List[Attribute[_]] = {
    import SemanticAttributes._

    List(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingSystem("aws_sqs")
    )
  }
}

object SqsRecordTraceAttributes {
  def apply(e: SqsRecord): List[Attribute[_]] = {
    import SemanticAttributes._

    List(
      FaasTrigger(FaasTriggerValue.Pubsub.value),
      MessagingOperation(MessagingOperationValue.Receive.value),
      MessagingMessageId(e.messageId),
      MessagingDestinationName(e.eventSource)
    )
  }
}
