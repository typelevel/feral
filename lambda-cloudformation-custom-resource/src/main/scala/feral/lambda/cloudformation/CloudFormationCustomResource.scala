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

import cats.ApplicativeThrow
import cats.MonadThrow
import cats.syntax.all._
import feral.lambda.cloudformation.CloudFormationRequestType._
import io.circe._
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

import java.io.PrintWriter
import java.io.StringWriter

trait CloudFormationCustomResource[F[_], Input, Output] {
  def createResource(input: Input): F[HandlerResponse[Output]]
  def updateResource(input: Input): F[HandlerResponse[Output]]
  def deleteResource(input: Input): F[HandlerResponse[Output]]
}

object CloudFormationCustomResource {

  def apply[F[_]: MonadThrow, Input, Output: Encoder](
      client: Client[F],
      handler: CloudFormationCustomResource[F, Input, Output])(
      implicit
      env: LambdaEnv[F, CloudFormationCustomResourceRequest[Input]]): F[Option[INothing]] = {
    val http4sClientDsl = new Http4sClientDsl[F] {}
    import http4sClientDsl._

    env.event.flatMap { event =>
      (event.RequestType match {
        case CreateRequest => handler.createResource(event)
        case UpdateRequest => handler.updateResource(event)
        case DeleteRequest => handler.deleteResource(event)
        case OtherRequestType(other) => illegalRequestType(other)
      }).attempt
        .map(_.fold(exceptionResponse(event)(_), successResponse(event)(_)))
        .flatMap { resp => client.successful(POST(resp.asJson, event.ResponseURL)) }
        .as(None)
    }
  }

  private def illegalRequestType[F[_]: ApplicativeThrow, A](other: String): F[A] =
    (new IllegalArgumentException(
      s"unexpected CloudFormation request type `$other``"): Throwable).raiseError[F, A]

  private def exceptionResponse[Input](req: CloudFormationCustomResourceRequest[Input])(
      ex: Throwable): CloudFormationCustomResourceResponse =
    CloudFormationCustomResourceResponse(
      Status = RequestResponseStatus.Failed,
      Reason = Option(ex.getMessage),
      PhysicalResourceId = req.PhysicalResourceId,
      StackId = req.StackId,
      RequestId = req.RequestId,
      LogicalResourceId = req.LogicalResourceId,
      Data = JsonObject(
        "StackTrace" -> Json.arr(stackTraceLines(ex).map(Json.fromString): _*)).asJson
    )

  private def successResponse[Input, Output: Encoder](
      req: CloudFormationCustomResourceRequest[Input])(
      res: HandlerResponse[Output]): CloudFormationCustomResourceResponse =
    CloudFormationCustomResourceResponse(
      Status = RequestResponseStatus.Success,
      Reason = None,
      PhysicalResourceId = Option(res.physicalId),
      StackId = req.StackId,
      RequestId = req.RequestId,
      LogicalResourceId = req.LogicalResourceId,
      Data = res.data.asJson
    )

  private def stackTraceLines(throwable: Throwable): List[String] = {
    val writer = new StringWriter()
    throwable.printStackTrace(new PrintWriter(writer))
    writer.toString.linesIterator.toList
  }

}
