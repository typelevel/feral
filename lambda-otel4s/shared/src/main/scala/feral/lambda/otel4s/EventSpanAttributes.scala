package feral.lambda.otel4s

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.SpanKind

protected trait EventSpanAttributes[E] {
  def contextCarrier(e: E): Map[String, String]
  def spanKind: SpanKind
  def attributes(e: E): List[Attribute[_]]
}
