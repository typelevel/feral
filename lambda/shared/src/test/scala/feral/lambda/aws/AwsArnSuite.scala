/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feral.lambda.aws

import munit.FunSuite

class AwsArnSuite extends FunSuite {

  test("AwsArn should create ARN from string") {
    val arnString = "arn:aws:s3:::my-bucket"
    val arn = AwsArn(arnString)
    assertEquals(arn.value, arnString)
  }

  test("AwsArn should parse valid ARN strings") {
    val arnString = "arn:aws:s3:::my-bucket"
    val arn = AwsArn(arnString)

    assertEquals(arn.value, arnString)
    assertEquals(AwsArn.service(arn), Some("s3"))
    assertEquals(AwsArn.region(arn), None)
    assertEquals(AwsArn.accountId(arn), None)
    assertEquals(AwsArn.resource(arn), Some("my-bucket"))
  }

  test("AwsArn should parse Lambda ARN with all components") {
    val lambdaArn = "arn:aws:lambda:us-east-1:123456789012:function:my-function"
    val arn = AwsArn(lambdaArn)

    assertEquals(arn.value, lambdaArn)
    assertEquals(AwsArn.service(arn), Some("lambda"))
    assertEquals(AwsArn.region(arn), Some("us-east-1"))
    assertEquals(AwsArn.accountId(arn), Some("123456789012"))
    assertEquals(AwsArn.resource(arn), Some("function:my-function"))
  }

  test("AwsArn should parse DynamoDB ARN") {
    val dynamoArn =
      "arn:aws:dynamodb:us-west-2:account-id:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899"
    val arn = AwsArn(dynamoArn)

    assertEquals(AwsArn.service(arn), Some("dynamodb"))
    assertEquals(AwsArn.region(arn), Some("us-west-2"))
    assertEquals(AwsArn.accountId(arn), Some("account-id"))
    assertEquals(
      AwsArn.resource(arn),
      Some("table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899"))
  }

  test("AwsArn should parse SQS ARN") {
    val sqsArn = "arn:aws:sqs:us-east-2:123456789012:my-queue"
    val arn = AwsArn(sqsArn)

    assertEquals(AwsArn.service(arn), Some("sqs"))
    assertEquals(AwsArn.region(arn), Some("us-east-2"))
    assertEquals(AwsArn.accountId(arn), Some("123456789012"))
    assertEquals(AwsArn.resource(arn), Some("my-queue"))
  }

  test("AwsArn should parse SNS ARN") {
    val snsArn = "arn:aws:sns:us-east-1:123456789012:my-topic"
    val arn = AwsArn(snsArn)

    assertEquals(AwsArn.service(arn), Some("sns"))
    assertEquals(AwsArn.region(arn), Some("us-east-1"))
    assertEquals(AwsArn.accountId(arn), Some("123456789012"))
    assertEquals(AwsArn.resource(arn), Some("my-topic"))
  }

  test("AwsArn should check service correctly") {
    val lambdaArn = AwsArn("arn:aws:lambda:us-east-1:123456789012:function:my-function")
    val s3Arn = AwsArn("arn:aws:s3:::my-bucket")

    assert(AwsArn.isService(lambdaArn, "lambda"))
    assert(!AwsArn.isService(lambdaArn, "s3"))
    assert(AwsArn.isService(s3Arn, "s3"))
    assert(!AwsArn.isService(s3Arn, "lambda"))
  }

  test("AwsArn should create ARN from components") {
    val components = AwsArnComponents(
      partition = "aws",
      service = "lambda",
      region = Some("us-east-1"),
      accountId = Some("123456789012"),
      resource = "function:my-function"
    )

    val arn = AwsArn.fromComponents(components)
    assertEquals(arn.value, "arn:aws:lambda:us-east-1:123456789012:function:my-function")
  }

  test("AwsArn should handle ARN with empty region and account") {
    val components = AwsArnComponents(
      partition = "aws",
      service = "s3",
      region = None,
      accountId = None,
      resource = "my-bucket"
    )

    val arn = AwsArn.fromComponents(components)
    assertEquals(arn.value, "arn:aws:s3:::my-bucket")
  }

  test("AwsArn should have smithy4s schema") {
    assert(AwsArn.schema != null)
    assertEquals(AwsArn.id.namespace, "feral.lambda.aws")
    assertEquals(AwsArn.id.name, "AwsArn")
  }

  test("AwsArnComponents should parse correctly") {
    val arnString = "arn:aws:lambda:us-east-1:123456789012:function:my-function"
    val components = AwsArn.parse(arnString)

    assert(components.isDefined)
    val comp = components.get
    assertEquals(comp.partition, "aws")
    assertEquals(comp.service, "lambda")
    assertEquals(comp.region, Some("us-east-1"))
    assertEquals(comp.accountId, Some("123456789012"))
    assertEquals(comp.resource, "function:my-function")
  }

  test("AwsArnComponents should return None for invalid ARN") {
    val invalidArn = "not-an-arn"
    val components = AwsArn.parse(invalidArn)

    assert(components.isEmpty)
  }
}
