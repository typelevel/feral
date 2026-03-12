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

package feral.lambda.events

import feral.lambda.events.events.APIGatewayCustomAuthorizerEventResult
import io.circe.literal._
import munit.FunSuite

class APIGatewayCustomAuthorizerEventResultSuite extends FunSuite {

  import APIGatewayCustomAuthorizerEventResultSuite._

  test("decoder") {
    event.as[APIGatewayCustomAuthorizerEventResult].toTry.get
  }
}

object APIGatewayCustomAuthorizerEventResultSuite {
  def event = json"""
    {
        "principalId": "me",
        "policyDocument": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": "execute-api:Invoke",
                    "Effect": "Allow",
                    "Resource": "arn:aws:execute-api:us-east-1:677276103099:or71kuogm2/test/GET/"
                }
            ]
        }
    }
    """
}
