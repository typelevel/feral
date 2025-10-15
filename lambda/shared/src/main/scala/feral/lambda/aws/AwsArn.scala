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

import smithy4s.Newtype
import smithy4s.ShapeId
import smithy4s.schema.Schema

/**
 * AWS ARN (Amazon Resource Name) type using smithy4s patterns. This demonstrates how smithy4s
 * can be integrated for AWS ARN handling.
 */
object AwsArn extends Newtype[String] {

  val id = ShapeId("feral.lambda.aws", "AwsArn")
  val schema = Schema.bijection(Schema.string, apply, value).withId(id)

  /**
   * Parse an ARN string and extract its components. ARN format:
   * arn:partition:service:region:account-id:resource
   */
  def parse(arn: String): Option[AwsArnComponents] = {
    val arnPattern = """arn:([^:]+):([^:]+):([^:]*):([^:]*):(.+)""".r
    arn match {
      case arnPattern(partition, service, region, accountId, resource) =>
        Some(
          AwsArnComponents(
            partition = partition,
            service = service,
            region = if (region.nonEmpty) Some(region) else None,
            accountId = if (accountId.nonEmpty) Some(accountId) else None,
            resource = resource
          ))
      case _ => None
    }
  }

  /**
   * Create an ARN from components
   */
  def fromComponents(components: AwsArnComponents): Type = {
    val region = components.region.getOrElse("")
    val accountId = components.accountId.getOrElse("")
    val arnString =
      s"arn:${components.partition}:${components.service}:$region:$accountId:${components.resource}"
    apply(arnString)
  }

  /**
   * Check if this ARN is for a specific AWS service
   */
  def isService(arn: Type, service: String): Boolean = {
    parse(arn.value).exists(_.service == service)
  }

  /**
   * Get the AWS service from this ARN
   */
  def service(arn: Type): Option[String] = {
    parse(arn.value).map(_.service)
  }

  /**
   * Get the AWS region from this ARN
   */
  def region(arn: Type): Option[String] = {
    parse(arn.value).flatMap(_.region)
  }

  /**
   * Get the account ID from this ARN
   */
  def accountId(arn: Type): Option[String] = {
    parse(arn.value).flatMap(_.accountId)
  }

  /**
   * Get the resource from this ARN
   */
  def resource(arn: Type): Option[String] = {
    parse(arn.value).map(_.resource)
  }

}

/**
 * Components of an AWS ARN
 */
case class AwsArnComponents(
    partition: String,
    service: String,
    region: Option[String],
    accountId: Option[String],
    resource: String
)
