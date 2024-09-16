package feral.lambda.otel4s

import munit.FunSuite
import org.typelevel.otel4s.semconv.experimental.attributes.FaasExperimentalAttributes
import org.typelevel.otel4s.semconv.experimental.attributes.CloudExperimentalAttributes

class LambdaContextAttributesTest extends FunSuite {

  import LambdaContextAttributes._
  val otel4s = FaasExperimentalAttributes
  val otel4sCloud = CloudExperimentalAttributes

  test("FaasInvocationId") {
    val value = "value"
    val ours = FaasInvocationId(value)
    val otel = otel4s.FaasInvocationId(value)
    assertEquals(ours, otel)
  }

  test("FaasTrigger") {
    val value = "value"
    val ours = FaasTrigger(value)
    val otel = otel4s.FaasTrigger(value)
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.PubSub") {
    val ours = FaasTriggerValue.Pubsub.value
    val otel = otel4s.FaasTriggerValue.Pubsub.value
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.Datasource") {
    val ours = FaasTriggerValue.Datasource.value
    val otel = otel4s.FaasTriggerValue.Datasource.value
    assertEquals(ours, otel)
  }

  test("FaasTriggerValue.Http") {
    val ours = FaasTriggerValue.Http.value
    val otel = otel4s.FaasTriggerValue.Http.value
    assertEquals(ours, otel)
  }

  test("CloudResourceId") {
    val value = "value"
    val ours = CloudResourceId(value)
    val otel = otel4sCloud.CloudResourceId(value)
    assertEquals(ours, otel)
  }

  test("FaasInstance") {
    val value = "value"
    val ours = FaasInstance(value)
    val otel = otel4s.FaasInstance(value)
    assertEquals(ours, otel)
  }

  test("FaasMaxMemory") {
    val value = 0L
    val ours = FaasMaxMemory(value)
    val otel = otel4s.FaasMaxMemory(value)
    assertEquals(ours, otel)
  }

  test("FaasName") {
    val value = "value"
    val ours = FaasName(value)
    val otel = otel4s.FaasName(value)
    assertEquals(ours, otel)
  }

  test("FaasVersion") {
    val value = "value"
    val ours = FaasVersion(value)
    val otel = otel4s.FaasVersion(value)
    assertEquals(ours, otel)
  }

  test("CloudProvider") {
    val value = "value"
    val ours = CloudProvider(value)
    val otel = otel4sCloud.CloudProvider(value)
    assertEquals(ours, otel)
  }

  test("CloudProviderValue.Aws") {
    val ours = CloudProviderValue.Aws.value
    val otel = otel4sCloud.CloudProviderValue.Aws.value
    assertEquals(ours, otel)
  }
}
