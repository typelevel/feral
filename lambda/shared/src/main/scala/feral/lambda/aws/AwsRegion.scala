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
 * AWS Region type using smithy4s patterns.
 * This implementation is based on smithy4s.aws.kernel.AwsRegion
 * and demonstrates integration with smithy4s for AWS types.
 */
object AwsRegion extends Newtype[String] {

  val id = ShapeId("feral.lambda.aws", "AwsRegion")
  val schema = Schema.bijection(Schema.string, apply, value).withId(id)

  // AWS Regions - copied from smithy4s.aws.kernel.AwsRegion
  val AF_SOUTH_1: Type = apply("af-south-1")
  val AP_EAST_1: Type = apply("ap-east-1")
  val AP_NORTHEAST_1: Type = apply("ap-northeast-1")
  val AP_NORTHEAST_2: Type = apply("ap-northeast-2")
  val AP_NORTHEAST_3: Type = apply("ap-northeast-3")
  val AP_SOUTH_1: Type = apply("ap-south-1")
  val AP_SOUTHEAST_1: Type = apply("ap-southeast-1")
  val AP_SOUTHEAST_2: Type = apply("ap-southeast-2")
  val CA_CENTRAL_1: Type = apply("ca-central-1")
  val CN_NORTH_1: Type = apply("cn-north-1")
  val CN_NORTHEAST_1: Type = apply("cn-northeast-1")
  val CN_NORTHWEST_1: Type = apply("cn-northwest-1")
  val EU_CENTRAL_1: Type = apply("eu-central-1")
  val EU_NORTH_1: Type = apply("eu-north-1")
  val EU_SOUTH_1: Type = apply("eu-south-1")
  val EU_WEST_1: Type = apply("eu-west-1")
  val EU_WEST_2: Type = apply("eu-west-2")
  val EU_WEST_3: Type = apply("eu-west-3")
  val ME_SOUTH_1: Type = apply("me-south-1")
  val SA_EAST_1: Type = apply("sa-east-1")
  val US_EAST_1: Type = apply("us-east-1")
  val US_EAST_2: Type = apply("us-east-2")
  val US_WEST_1: Type = apply("us-west-1")
  val US_WEST_2: Type = apply("us-west-2")

  // Additional regions for government and isolated clouds
  val GovCloud: Type = apply("govcloud")
  val US_GOV_EAST_1: Type = apply("us-gov-east-1")
  val US_GOV_WEST_1: Type = apply("us-gov-west-1")
  val US_ISO_EAST_1: Type = apply("us-iso-east-1")
  val US_ISO_WEST_1: Type = apply("us-iso-west-1")
  val US_ISOB_EAST_1: Type = apply("us-isob-east-1")
  val US_ISOB_WEST_1: Type = apply("us-isob-west-1")

}
