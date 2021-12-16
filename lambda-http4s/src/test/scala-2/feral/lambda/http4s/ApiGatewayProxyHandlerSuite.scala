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

package feral.lambda.http4s

import cats.effect.IO
import cats.syntax.all._
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.ApiGatewayProxyEventV2Suite
import munit.FunSuite

class ApiGatewayProxyHandlerSuite extends FunSuite {

  import ApiGatewayProxyEventV2Suite._

  test("decode event") {
    for {
      event <- event.as[ApiGatewayProxyEventV2].liftTo[IO]
      _ <- ApiGatewayProxyHandler.decodeEvent[IO](event)
    } yield () // TODO actually validate the output :)
  }

}
