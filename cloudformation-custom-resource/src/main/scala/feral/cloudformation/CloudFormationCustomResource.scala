package feral.cloudformation

import cats.effect._
import cats.effect.kernel.Resource
import cats.syntax.all._
import feral.cloudformation.CloudFormationCustomResourceHandler.stackTraceLines
import feral.cloudformation.CloudFormationRequestType._
import feral.lambda.{Context, IOLambda}
import io.circe._
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.ember.client.EmberClientBuilder

import java.io.{PrintWriter, StringWriter}

trait CloudFormationCustomResource[F[_], Input, Output] {
  def createResource(event: CloudFormationCustomResourceRequest[Input]): F[HandlerResponse[Output]]
  def updateResource(event: CloudFormationCustomResourceRequest[Input]): F[HandlerResponse[Output]]
  def deleteResource(event: CloudFormationCustomResourceRequest[Input]): F[HandlerResponse[Output]]
}

abstract class CloudFormationCustomResourceHandler[Input : Decoder, Output: Encoder]
  extends IOLambda[CloudFormationCustomResourceRequest[Input], Unit] {
  type Setup = (Client[IO], CloudFormationCustomResource[IO, Input, Output])

  override final def setup: Resource[IO, Setup] =
    client.mproduct(handler)

  protected def client: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  def handler(client: Client[IO]): Resource[IO, CloudFormationCustomResource[IO, Input, Output]]

  override def apply(event: CloudFormationCustomResourceRequest[Input],
                     context: Context,
                     setup: Setup): IO[Option[Unit]] =
    (event.RequestType match {
      case CreateRequest => setup._2.createResource(event)
      case UpdateRequest => setup._2.updateResource(event)
      case DeleteRequest => setup._2.deleteResource(event)
      case OtherRequestType(other) => illegalRequestType(other)
    })
      .attempt
      .map(_.fold(exceptionResponse(event)(_), successResponse(event)(_)))
      .flatMap { resp =>
        setup._1.successful(POST(resp.asJson, event.ResponseURL))
      }
      .as(None)

  private def illegalRequestType[A](other: String): IO[A] =
    (new IllegalArgumentException(s"unexpected CloudFormation request type `$other``"): Throwable).raiseError[IO, A]

  private def exceptionResponse(req: CloudFormationCustomResourceRequest[Input])
                               (ex: Throwable): CloudFormationCustomResourceResponse =
    CloudFormationCustomResourceResponse(
      Status = RequestResponseStatus.Failed,
      Reason = Option(ex.getMessage),
      PhysicalResourceId = req.PhysicalResourceId,
      StackId = req.StackId,
      RequestId = req.RequestId,
      LogicalResourceId = req.LogicalResourceId,
      Data = JsonObject("StackTrace" -> Json.arr(stackTraceLines(ex).map(Json.fromString): _*)).asJson
    )

  private def successResponse(req: CloudFormationCustomResourceRequest[Input])
                             (res: HandlerResponse[Output]): CloudFormationCustomResourceResponse =
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
