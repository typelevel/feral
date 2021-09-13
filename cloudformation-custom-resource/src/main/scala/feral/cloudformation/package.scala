package feral

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import monix.newtypes._
import org.http4s.Uri
import org.http4s.circe.CirceInstances

package object cloudformation {
  type PhysicalResourceId = PhysicalResourceId.Type
  type StackId = StackId.Type
  type RequestId = RequestId.Type
  type LogicalResourceId = LogicalResourceId.Type
  type ResourceType = ResourceType.Type
}

package cloudformation {

  object PhysicalResourceId extends NewtypeWrapped[String] {
    implicit val PhysicalResourceIdDecoder: Decoder[PhysicalResourceId] = derive[Decoder]
    implicit val PhysicalResourceIdEncoder: Encoder[PhysicalResourceId] = derive[Encoder]
  }
  object StackId extends NewtypeWrapped[String] {
    implicit val StackIdDecoder: Decoder[StackId] = derive[Decoder]
    implicit val StackIdEncoder: Encoder[StackId] = derive[Encoder]
  }
  object RequestId extends NewtypeWrapped[String] {
    implicit val RequestIdDecoder: Decoder[RequestId] = derive[Decoder]
    implicit val RequestIdEncoder: Encoder[RequestId] = derive[Encoder]
  }
  object LogicalResourceId extends NewtypeWrapped[String] {
    implicit val LogicalResourceIdDecoder: Decoder[LogicalResourceId] = derive[Decoder]
    implicit val LogicalResourceIdEncoder: Encoder[LogicalResourceId] = derive[Encoder]
  }
  object ResourceType extends NewtypeWrapped[String] {
    implicit val ResourceTypeDecoder: Decoder[ResourceType] = derive[Decoder]
    implicit val ResourceTypeEncoder: Encoder[ResourceType] = derive[Encoder]
  }

  sealed trait CloudFormationRequestType
  object CloudFormationRequestType {
    case object CreateRequest extends CloudFormationRequestType
    case object UpdateRequest extends CloudFormationRequestType
    case object DeleteRequest extends CloudFormationRequestType
    case class OtherRequestType(requestType: String) extends CloudFormationRequestType

    implicit val encoder: Encoder[CloudFormationRequestType] = {
      case CreateRequest => "Create".asJson
      case UpdateRequest => "Update".asJson
      case DeleteRequest => "Delete".asJson
      case OtherRequestType(req) => req.asJson
    }

    implicit val decoder: Decoder[CloudFormationRequestType] = Decoder[String].map {
      case "Create" => CreateRequest
      case "Update" => UpdateRequest
      case "Delete" => DeleteRequest
      case other => OtherRequestType(other)
    }
  }

  sealed trait RequestResponseStatus
  object RequestResponseStatus {
    case object Success extends RequestResponseStatus
    case object Failed extends RequestResponseStatus

    implicit val encoder: Encoder[RequestResponseStatus] = {
      case Success => "SUCCESS".asJson
      case Failed => "FAILED".asJson
    }

    implicit val decoder: Decoder[RequestResponseStatus] = Decoder[String].emap {
      case "SUCCESS" => Success.asRight
      case "FAILED" => Failed.asRight
      case other => s"Invalid response status: $other".asLeft
    }
  }

  case class CloudFormationCustomResourceRequest[A](RequestType: CloudFormationRequestType,
                                                    ResponseURL: Uri,
                                                    StackId: StackId,
                                                    RequestId: RequestId,
                                                    ResourceType: ResourceType,
                                                    LogicalResourceId: LogicalResourceId,
                                                    PhysicalResourceId: Option[PhysicalResourceId],
                                                    ResourceProperties: A,
                                                    OldResourceProperties: Option[JsonObject])

  object CloudFormationCustomResourceRequest extends CirceInstances {
    implicit def CloudFormationCustomResourceRequestDecoder[A: Decoder]: Decoder[CloudFormationCustomResourceRequest[A]] = deriveDecoder[CloudFormationCustomResourceRequest[A]]
    implicit def CloudFormationCustomResourceRequestEncoder[A: Encoder]: Encoder[CloudFormationCustomResourceRequest[A]] = deriveEncoder[CloudFormationCustomResourceRequest[A]]
  }

  case class CloudFormationCustomResourceResponse(Status: RequestResponseStatus,
                                                  Reason: Option[String],
                                                  PhysicalResourceId: Option[PhysicalResourceId],
                                                  StackId: StackId,
                                                  RequestId: RequestId,
                                                  LogicalResourceId: LogicalResourceId,
                                                  Data: Json)

  object CloudFormationCustomResourceResponse {
    implicit val CloudFormationCustomResourceResponseDecoder: Decoder[CloudFormationCustomResourceResponse] = deriveDecoder[CloudFormationCustomResourceResponse]
    implicit val CloudFormationCustomResourceResponseEncoder: Encoder[CloudFormationCustomResourceResponse] = deriveEncoder[CloudFormationCustomResourceResponse]
  }

  case class HandlerResponse[A](physicalId: PhysicalResourceId,
                                data: Option[A])

  object HandlerResponse {
    implicit def HandlerResponseCodec[A: Encoder : Decoder]: Codec[HandlerResponse[A]] = deriveCodec[HandlerResponse[A]]
  }

  object MissingResourceProperties extends RuntimeException
}
