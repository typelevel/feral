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

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
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

  object PhysicalResourceId extends Newtype[String] {

    /**
     * Applies validation rules from
     * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/crpg-ref-responses.html
     */
    def apply(s: String): Option[Type] =
      Option.when(s.length <= 1024 && s.nonEmpty)(unsafeCoerce(s))

    def unsafeApply(s: String): PhysicalResourceId = unsafeCoerce(s)

    def unapply[A](a: A)(implicit ev: A =:= Type): Some[String] =
      Some(value(ev(a)))

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
    final case class OtherRequestType(requestType: String) extends CloudFormationRequestType {
      override def toString: String = requestType
    }

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

  final case class CloudFormationCustomResourceRequest[A](
      RequestType: CloudFormationRequestType,
      ResponseURL: Uri,
      StackId: StackId,
      RequestId: RequestId,
      ResourceType: ResourceType,
      LogicalResourceId: LogicalResourceId,
      PhysicalResourceId: Option[PhysicalResourceId],
      ResourceProperties: A,
      OldResourceProperties: Option[JsonObject])

  object CloudFormationCustomResourceRequest extends CirceInstances {
    implicit def CloudFormationCustomResourceRequestDecoder[A: Decoder]
        : Decoder[CloudFormationCustomResourceRequest[A]] =
      Decoder.forProduct9(
        "RequestType",
        "ResponseURL",
        "StackId",
        "RequestId",
        "ResourceType",
        "LogicalResourceId",
        "PhysicalResourceId",
        "ResourceProperties",
        "OldResourceProperties"
      )(CloudFormationCustomResourceRequest.apply[A])

    implicit def CloudFormationCustomResourceRequestEncoder[A: Encoder]
        : Encoder[CloudFormationCustomResourceRequest[A]] =
      Encoder.forProduct9(
        "RequestType",
        "ResponseURL",
        "StackId",
        "RequestId",
        "ResourceType",
        "LogicalResourceId",
        "PhysicalResourceId",
        "ResourceProperties",
        "OldResourceProperties"
      ) { r =>
        (
          r.RequestType,
          r.ResponseURL,
          r.StackId,
          r.RequestId,
          r.ResourceType,
          r.LogicalResourceId,
          r.PhysicalResourceId,
          r.ResourceProperties,
          r.OldResourceProperties
        )
      }
  }

  final case class CloudFormationCustomResourceResponse(
      Status: RequestResponseStatus,
      Reason: Option[String],
      PhysicalResourceId: Option[PhysicalResourceId],
      StackId: StackId,
      RequestId: RequestId,
      LogicalResourceId: LogicalResourceId,
      Data: Json)

  object CloudFormationCustomResourceResponse {
    implicit val CloudFormationCustomResourceResponseDecoder
        : Decoder[CloudFormationCustomResourceResponse] =
      Decoder
        .forProduct7(
          "Status",
          "Reason",
          "PhysicalResourceId",
          "StackId",
          "RequestId",
          "LogicalResourceId",
          "Data"
        )(CloudFormationCustomResourceResponse.apply)
        .prepare {
          _.withFocus {
            _.mapObject { obj =>
              if (obj.contains("Data")) obj
              else obj.add("Data", Json.Null)
            }
          }
        }

    implicit val CloudFormationCustomResourceResponseEncoder
        : Encoder[CloudFormationCustomResourceResponse] =
      Encoder.forProduct7(
        "Status",
        "Reason",
        "PhysicalResourceId",
        "StackId",
        "RequestId",
        "LogicalResourceId",
        "Data"
      ) { r =>
        (
          r.Status,
          r.Reason,
          r.PhysicalResourceId,
          r.StackId,
          r.RequestId,
          r.LogicalResourceId,
          r.Data
        )
      }
  }

  final case class HandlerResponse[A](physicalId: PhysicalResourceId, data: Option[A])

  object HandlerResponse {
    implicit def HandlerResponseCodec[A: Encoder: Decoder]: Codec[HandlerResponse[A]] =
      Codec.forProduct2("PhysicalResourceId", "Data")(HandlerResponse.apply[A]) { r =>
        (r.physicalId, r.data)
      }
  }

  object MissingResourceProperties extends RuntimeException
}
