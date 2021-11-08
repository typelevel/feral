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

package feral.lambda.cloudformation

import cats.effect.{IO, Resource}
import cats.syntax.all._
import feral.lambda.cloudformation.CloudFormationCustomResourceHandler.stackTraceLines
import feral.lambda.cloudformation.CloudFormationRequestType._
import feral.lambda.{Context, IOLambda}
import io.circe._
import io.circe.syntax._
import natchez.Trace
import org.http4s.Method.POST
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.ember.client.EmberClientBuilder

import java.io.{PrintWriter, StringWriter}

trait CloudFormationCustomResource[F[_], Input, Output] {
  def createResource(
      event: CloudFormationCustomResourceRequest[Input],
      context: Context): F[HandlerResponse[Output]]
  def updateResource(
      event: CloudFormationCustomResourceRequest[Input],
      context: Context): F[HandlerResponse[Output]]
  def deleteResource(
      event: CloudFormationCustomResourceRequest[Input],
      context: Context): F[HandlerResponse[Output]]
}

abstract class CloudFormationCustomResourceHandler[Input: Decoder, Output: Encoder]
    extends IOLambda[CloudFormationCustomResourceRequest[Input], Unit] {
  type Setup = (Client[IO], CloudFormationCustomResource[IO, Input, Output])

  override final def setup: Resource[IO, Setup] =
    client.mproduct(handler)

  protected def client: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  def handler(client: Client[IO]): Resource[IO, CloudFormationCustomResource[IO, Input, Output]]

  override def apply(
      event: CloudFormationCustomResourceRequest[Input],
      context: Context,
      setup: Setup)(implicit T: Trace[IO]): IO[Option[Unit]] =
    (event.RequestType match {
      case CreateRequest => setup._2.createResource(event, context)
      case UpdateRequest => setup._2.updateResource(event, context)
      case DeleteRequest => setup._2.deleteResource(event, context)
      case OtherRequestType(other) => illegalRequestType(other)
    }).attempt
      .map(_.fold(exceptionResponse(event)(_), successResponse(event)(_)))
      .flatMap { resp => setup._1.successful(POST(resp.asJson, event.ResponseURL)) }
      .as(None)

  private def illegalRequestType[A](other: String): IO[A] =
    (new IllegalArgumentException(
      s"unexpected CloudFormation request type `$other``"): Throwable).raiseError[IO, A]

  private def exceptionResponse(req: CloudFormationCustomResourceRequest[Input])(
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

  private def successResponse(req: CloudFormationCustomResourceRequest[Input])(
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

}

object CloudFormationCustomResourceHandler {
  def stackTraceLines(throwable: Throwable): List[String] = {
    val writer = new StringWriter()
    throwable.printStackTrace(new PrintWriter(writer))
    writer.toString.linesIterator.toList
  }
}
