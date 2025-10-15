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

class AwsRegionSuite extends FunSuite {
  
  test("AwsRegion should create region from string") {
    val region = AwsRegion("us-east-1")
    assertEquals(region.value, "us-east-1")
  }
  
  test("AwsRegion should provide common regions") {
    assertEquals(AwsRegion.US_EAST_1.value, "us-east-1")
    assertEquals(AwsRegion.US_WEST_2.value, "us-west-2")
    assertEquals(AwsRegion.EU_WEST_1.value, "eu-west-1")
    assertEquals(AwsRegion.AP_SOUTHEAST_1.value, "ap-southeast-1")
  }
  
  test("AwsRegion should provide government regions") {
    assertEquals(AwsRegion.US_GOV_EAST_1.value, "us-gov-east-1")
    assertEquals(AwsRegion.US_GOV_WEST_1.value, "us-gov-west-1")
  }
  
  test("AwsRegion should provide isolated cloud regions") {
    assertEquals(AwsRegion.US_ISO_EAST_1.value, "us-iso-east-1")
    assertEquals(AwsRegion.US_ISOB_EAST_1.value, "us-isob-east-1")
  }
  
  test("AwsRegion should have smithy4s schema") {
    assert(AwsRegion.schema != null)
    assertEquals(AwsRegion.id.namespace, "feral.lambda.aws")
    assertEquals(AwsRegion.id.name, "AwsRegion")
  }
}
