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
import io.circe.Json
import io.circe.jawn.CirceSupportParser.facade
import io.circe.literal._
import io.circe.optics.JsonPath.root
import io.circe.syntax._
import munit._
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.io._
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

  /**
   * Only compare the head of the stack trace array, because the rest of the stack trace is
   * expected to vary too much
   */
  private val trimStackTrace = root.Data.StackTrace.arr.modify(_.take(1))

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
          event.RequestType match {
            case OtherRequestType(requestType) =>
              val expectedReason = s"unexpected CloudFormation request type `$requestType`"
              val expectedStackTraceHead =
                s"java.lang.IllegalArgumentException: $expectedReason"
              val expectedJson =
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

              val bodyWithTrimmedStackTrace = trimStackTrace(body)
              expect(bodyWithTrimmedStackTrace eqv expectedJson)
            case _ =>
              val expectedJson = Json.obj(
                "Status" -> "SUCCESS".asJson,
                "PhysicalResourceId" -> event.ResourceProperties.asJson,
                "StackId" -> event.StackId.asJson,
                "RequestId" -> event.RequestId.asJson,
                "LogicalResourceId" -> event.LogicalResourceId.asJson,
                "Data" -> event.RequestType.asJson
              )

              // Use `eqv` here because we want to make sure nulls are dropped in the body we received
              expect(body eqv expectedJson)
          }

          expect(req.method == PUT)
          expect(req.uri == event.ResponseURL)
        }
    }
  }
}

class DoNothingCustomResource[F[_]: Applicative]
    extends CloudFormationCustomResource[F, String, CloudFormationRequestType] {
  override def createResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
    HandlerResponse(
      PhysicalResourceId(input),
      CreateRequest.some.widen[CloudFormationRequestType]).pure[F]
  override def updateResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
    HandlerResponse(
      PhysicalResourceId(input),
      UpdateRequest.some.widen[CloudFormationRequestType]).pure[F]
  override def deleteResource(input: String): F[HandlerResponse[CloudFormationRequestType]] =
    HandlerResponse(
      PhysicalResourceId(input),
      DeleteRequest.some.widen[CloudFormationRequestType]).pure[F]
}
