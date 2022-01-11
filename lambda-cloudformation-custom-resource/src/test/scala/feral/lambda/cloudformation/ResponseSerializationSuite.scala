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

package feral.lambda
package cloudformation

import cats._
import cats.effect._
import cats.syntax.all._
import com.eed3si9n.expecty.Expecty.expect
import feral.lambda.cloudformation.CloudFormationRequestType._
import feral.lambda.cloudformation.ResponseSerializationSuite._
import io.circe.Json
import io.circe.jawn.CirceSupportParser.facade
import io.circe.literal._
import munit._
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Length`
import org.scalacheck.effect.PropF
import org.typelevel.jawn.Parser

import scala.concurrent.duration._

class ResponseSerializationSuite
    extends CatsEffectSuite
    with ScalaCheckEffectSuite
    with CloudFormationCustomResourceArbitraries {

  private def captureRequestsAndRespondWithOk(
      capture: Deferred[IO, (Request[IO], Json)]): HttpApp[IO] =
    HttpApp { req =>
      for {
        body <- req.body.compile.to(Array).flatMap(Parser.parseFromByteArray(_).liftTo[IO])
        _ <- capture.complete((req, body))
        resp <- Ok()
      } yield resp
    }

  test("CloudFormationCustomResource should PUT the response to the given URI") {
    PropF.forAllF {
      implicit lambdaEnv: LambdaEnv[IO, CloudFormationCustomResourceRequest[String]] =>
        for {
          eventualRequest <- Deferred[IO, (Request[IO], Json)]
          client = Client.fromHttpApp(captureRequestsAndRespondWithOk(eventualRequest))
          _ <- CloudFormationCustomResource(client, new DoNothingCustomResource[IO])
          (req, body) <- eventualRequest.get.timeout(2.seconds)
          event <- lambdaEnv.event
        } yield {
          val expectedJson = event.RequestType match {
            case OtherRequestType(requestType) =>
              val expectedReason = s"unexpected CloudFormation request type `$requestType`"
              val expectedStackTraceHead =
                s"java.lang.IllegalArgumentException: $expectedReason"
              json"""{
                       "Status": "FAILED",
                       "Reason": $expectedReason,
                       "PhysicalResourceId": ${event.PhysicalResourceId},
                       "StackId": ${event.StackId},
                       "RequestId": ${event.RequestId},
                       "LogicalResourceId": ${event.LogicalResourceId},
                       "Data": {
                         "StackTrace": [
                           $expectedStackTraceHead
                         ]
                       }
                     }""".deepDropNullValues
            case _ =>
              json"""{
                       "Status": "SUCCESS",
                       "PhysicalResourceId": ${convertInputToFakePhysicalResourceId(
                event.ResourceProperties)},
                       "StackId": ${event.StackId},
                       "RequestId": ${event.RequestId},
                       "LogicalResourceId": ${event.LogicalResourceId},
                       "Data": ${event.RequestType}
                     }"""
          }

          expect(body eqv expectedJson)
          expect(req.method == PUT)
          expect(req.uri == event.ResponseURL)
          expect(req.headers.get[`Content-Length`].exists(_.length <= 4096))
        }
    }
  }
}

object ResponseSerializationSuite {
  def convertInputToFakePhysicalResourceId(input: String): PhysicalResourceId =
    PhysicalResourceId(input).getOrElse(
      PhysicalResourceId.unsafeApply(s"input `$input` was an invalid PhysicalResourceId"))

  class DoNothingCustomResource[F[_]: Applicative]
      extends CloudFormationCustomResource[F, String, CloudFormationRequestType] {
    override def createResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
      HandlerResponse(
        convertInputToFakePhysicalResourceId(input),
        CreateRequest.some.widen[CloudFormationRequestType]).pure[F]

    override def updateResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
      HandlerResponse(
        convertInputToFakePhysicalResourceId(input),
        UpdateRequest.some.widen[CloudFormationRequestType]).pure[F]

    override def deleteResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
      HandlerResponse(
        convertInputToFakePhysicalResourceId(input),
        DeleteRequest.some.widen[CloudFormationRequestType]).pure[F]
  }
}
