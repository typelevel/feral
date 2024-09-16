package feral.lambda.otel4s

import feral.lambda.Context
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.AttributeKey

/**
 * Temporary aliases for Lambda message-specific attributes in otel4s-semconv-experimental
 */
private[otel4s] object LambdaMessageAttributes {
  val MessagingSystem = AttributeKey.string("messaging.system")
  object MessagingSystemValue {
    object Sqs {
      val value = "aws_sqs"
    }
  }
  val MessagingOperation = AttributeKey.string("messaging.operation")
  object MessagingOperationValue {
    object Receive {
      val value = "receive"
    }
  }
  val MessagingMessageId = AttributeKey.string("messaging.message.id")
  val MessagingDestinationName = AttributeKey.string("messaging.destination.name")
}
