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
package events

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.HCursor

sealed abstract class CognitoUserPoolEvent {
  def version: String
  def triggerSource: String
  def region: String
  def userPoolId: String
  def userName: String
  def callerContext: CognitoUserPoolCallerContext
}

object CognitoUserPoolEvent {
  implicit val decoder: Decoder[CognitoUserPoolEvent] = Decoder.instance { i =>
    i.get[String]("triggerSource").flatMap {
      case source if source.startsWith("CreateAuthChallenge_") =>
        i.as[CognitoUserPoolCreateAuthChallengeEvent]
      case source if source.startsWith("CustomMessage_") =>
        i.as[CognitoUserPoolCustomMessageEvent]
      case source if source.startsWith("DefineAuthChallenge_") =>
        i.as[CognitoUserPoolDefineAuthChallengeEvent]
      case source if source.startsWith("UserMigration_") =>
        i.as[CognitoUserPoolMigrateUserEvent]
      case source if source.startsWith("PostAuthentication_") =>
        i.as[CognitoUserPoolPostAuthenticationEvent]
      case source if source.startsWith("PostConfirmation_") =>
        i.as[CognitoUserPoolPostConfirmationEvent]
      case source if source.startsWith("PreAuthentication_") =>
        i.as[CognitoUserPoolPreAuthenticationEvent]
      case source if source.startsWith("PreSignUp_") =>
        i.as[CognitoUserPoolPreSignUpEvent]
      case source if source.startsWith("TokenGeneration_") =>
        i.as[CognitoUserPoolPreTokenGenerationEvent]
      case source if source.startsWith("VerifyAuthChallengeResponse_") =>
        i.as[CognitoUserPoolVerifyAuthChallengeResponseEvent]
      case source =>
        Left(DecodingFailure(s"Unsupported Cognito triggerSource: $source", i.history))
    }
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolEvent] =
    KernelSource.emptyKernelSource

  private[events] def decodeEventData(i: HCursor): Decoder.Result[CognitoUserPoolEventData] =
    for {
      version <- i.get[String]("version")
      triggerSource <- i.get[String]("triggerSource")
      region <- i.get[String]("region")
      userPoolId <- i.get[String]("userPoolId")
      userName <- i.get[String]("userName")
      callerContext <- i.get[CognitoUserPoolCallerContext]("callerContext")
    } yield CognitoUserPoolEventData(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext
    )
}

private[events] final case class CognitoUserPoolEventData(
    version: String,
    triggerSource: String,
    region: String,
    userPoolId: String,
    userName: String,
    callerContext: CognitoUserPoolCallerContext
)

sealed abstract class CognitoUserPoolCallerContext {
  def awsSdkVersion: String
  def clientId: String
}

object CognitoUserPoolCallerContext {
  def apply(awsSdkVersion: String, clientId: String): CognitoUserPoolCallerContext =
    new Impl(awsSdkVersion, clientId)

  private[events] implicit val decoder: Decoder[CognitoUserPoolCallerContext] =
    Decoder.forProduct2("awsSdkVersion", "clientId")(CognitoUserPoolCallerContext.apply)

  private final case class Impl(
      awsSdkVersion: String,
      clientId: String
  ) extends CognitoUserPoolCallerContext {
    override def productPrefix = "CognitoUserPoolCallerContext"
  }
}

sealed abstract class CognitoUserPoolCreateAuthChallengeEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolCreateAuthChallengeRequest
  def response: CognitoUserPoolCreateAuthChallengeResponse
}

object CognitoUserPoolCreateAuthChallengeEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolCreateAuthChallengeRequest,
      response: CognitoUserPoolCreateAuthChallengeResponse
  ): CognitoUserPoolCreateAuthChallengeEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolCreateAuthChallengeEvent] = Decoder.instance {
    i =>
      for {
        event <- CognitoUserPoolEvent.decodeEventData(i)
        request <- i.get[CognitoUserPoolCreateAuthChallengeRequest]("request")
        response <- i.get[CognitoUserPoolCreateAuthChallengeResponse]("response")
      } yield CognitoUserPoolCreateAuthChallengeEvent(
        event.version,
        event.triggerSource,
        event.region,
        event.userPoolId,
        event.userName,
        event.callerContext,
        request,
        response
      )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolCreateAuthChallengeEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolCreateAuthChallengeRequest,
      response: CognitoUserPoolCreateAuthChallengeResponse
  ) extends CognitoUserPoolCreateAuthChallengeEvent {
    override def productPrefix = "CognitoUserPoolCreateAuthChallengeEvent"
  }
}

sealed abstract class CognitoUserPoolCreateAuthChallengeRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def challengeName: Option[String]
  def session: List[CognitoUserPoolChallengeResult]
  def userNotFound: Boolean
}

object CognitoUserPoolCreateAuthChallengeRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      challengeName: Option[String],
      session: List[CognitoUserPoolChallengeResult],
      userNotFound: Boolean
  ): CognitoUserPoolCreateAuthChallengeRequest =
    new Impl(userAttributes, clientMetadata, challengeName, session, userNotFound)

  private[events] implicit val decoder: Decoder[CognitoUserPoolCreateAuthChallengeRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        challengeName <- i.get[Option[String]]("challengeName")
        session <- i.get[Option[List[CognitoUserPoolChallengeResult]]]("session")
        userNotFound <- CognitoUserPoolEventCodecs.decodeBoolean(i, "userNotFound")
      } yield CognitoUserPoolCreateAuthChallengeRequest(
        userAttributes,
        clientMetadata,
        challengeName,
        session.getOrElse(Nil),
        userNotFound
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      challengeName: Option[String],
      session: List[CognitoUserPoolChallengeResult],
      userNotFound: Boolean
  ) extends CognitoUserPoolCreateAuthChallengeRequest {
    override def productPrefix = "CognitoUserPoolCreateAuthChallengeRequest"
  }
}

sealed abstract class CognitoUserPoolChallengeResult {
  def challengeName: Option[String]
  def challengeResult: Boolean
  def challengeMetadata: Option[String]
}

object CognitoUserPoolChallengeResult {
  def apply(
      challengeName: Option[String],
      challengeResult: Boolean,
      challengeMetadata: Option[String]
  ): CognitoUserPoolChallengeResult =
    new Impl(challengeName, challengeResult, challengeMetadata)

  private[events] implicit val decoder: Decoder[CognitoUserPoolChallengeResult] =
    Decoder.instance { i =>
      for {
        challengeName <- i.get[Option[String]]("challengeName")
        challengeResult <- CognitoUserPoolEventCodecs.decodeBoolean(i, "challengeResult")
        challengeMetadata <- i.get[Option[String]]("challengeMetadata")
      } yield CognitoUserPoolChallengeResult(challengeName, challengeResult, challengeMetadata)
    }

  private final case class Impl(
      challengeName: Option[String],
      challengeResult: Boolean,
      challengeMetadata: Option[String]
  ) extends CognitoUserPoolChallengeResult {
    override def productPrefix = "CognitoUserPoolChallengeResult"
  }
}

sealed abstract class CognitoUserPoolCreateAuthChallengeResponse {
  def publicChallengeParameters: Map[String, String]
  def privateChallengeParameters: Map[String, String]
  def challengeMetadata: Option[String]
}

object CognitoUserPoolCreateAuthChallengeResponse {
  def apply(
      publicChallengeParameters: Map[String, String],
      privateChallengeParameters: Map[String, String],
      challengeMetadata: Option[String]
  ): CognitoUserPoolCreateAuthChallengeResponse =
    new Impl(publicChallengeParameters, privateChallengeParameters, challengeMetadata)

  private[events] implicit val decoder: Decoder[CognitoUserPoolCreateAuthChallengeResponse] =
    Decoder.instance { i =>
      for {
        publicChallengeParameters <- CognitoUserPoolEventCodecs.decodeMap(
          i,
          "publicChallengeParameters")
        privateChallengeParameters <- CognitoUserPoolEventCodecs.decodeMap(
          i,
          "privateChallengeParameters")
        challengeMetadata <- i.get[Option[String]]("challengeMetadata")
      } yield CognitoUserPoolCreateAuthChallengeResponse(
        publicChallengeParameters,
        privateChallengeParameters,
        challengeMetadata
      )
    }

  private final case class Impl(
      publicChallengeParameters: Map[String, String],
      privateChallengeParameters: Map[String, String],
      challengeMetadata: Option[String]
  ) extends CognitoUserPoolCreateAuthChallengeResponse {
    override def productPrefix = "CognitoUserPoolCreateAuthChallengeResponse"
  }
}

sealed abstract class CognitoUserPoolCustomMessageEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolCustomMessageRequest
  def response: CognitoUserPoolCustomMessageResponse
}

object CognitoUserPoolCustomMessageEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolCustomMessageRequest,
      response: CognitoUserPoolCustomMessageResponse
  ): CognitoUserPoolCustomMessageEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolCustomMessageEvent] = Decoder.instance { i =>
    for {
      event <- CognitoUserPoolEvent.decodeEventData(i)
      request <- i.get[CognitoUserPoolCustomMessageRequest]("request")
      response <- i.get[CognitoUserPoolCustomMessageResponse]("response")
    } yield CognitoUserPoolCustomMessageEvent(
      event.version,
      event.triggerSource,
      event.region,
      event.userPoolId,
      event.userName,
      event.callerContext,
      request,
      response
    )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolCustomMessageEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolCustomMessageRequest,
      response: CognitoUserPoolCustomMessageResponse
  ) extends CognitoUserPoolCustomMessageEvent {
    override def productPrefix = "CognitoUserPoolCustomMessageEvent"
  }
}

sealed abstract class CognitoUserPoolCustomMessageRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def codeParameter: Option[String]
  def usernameParameter: Option[String]
}

object CognitoUserPoolCustomMessageRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      codeParameter: Option[String],
      usernameParameter: Option[String]
  ): CognitoUserPoolCustomMessageRequest =
    new Impl(userAttributes, clientMetadata, codeParameter, usernameParameter)

  private[events] implicit val decoder: Decoder[CognitoUserPoolCustomMessageRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        codeParameter <- i.get[Option[String]]("codeParameter")
        usernameParameter <- i.get[Option[String]]("usernameParameter")
      } yield CognitoUserPoolCustomMessageRequest(
        userAttributes,
        clientMetadata,
        codeParameter,
        usernameParameter
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      codeParameter: Option[String],
      usernameParameter: Option[String]
  ) extends CognitoUserPoolCustomMessageRequest {
    override def productPrefix = "CognitoUserPoolCustomMessageRequest"
  }
}

sealed abstract class CognitoUserPoolCustomMessageResponse {
  def smsMessage: Option[String]
  def emailMessage: Option[String]
  def emailSubject: Option[String]
}

object CognitoUserPoolCustomMessageResponse {
  def apply(
      smsMessage: Option[String],
      emailMessage: Option[String],
      emailSubject: Option[String]
  ): CognitoUserPoolCustomMessageResponse =
    new Impl(smsMessage, emailMessage, emailSubject)

  private[events] implicit val decoder: Decoder[CognitoUserPoolCustomMessageResponse] =
    Decoder.forProduct3("smsMessage", "emailMessage", "emailSubject")(
      CognitoUserPoolCustomMessageResponse.apply)

  private final case class Impl(
      smsMessage: Option[String],
      emailMessage: Option[String],
      emailSubject: Option[String]
  ) extends CognitoUserPoolCustomMessageResponse {
    override def productPrefix = "CognitoUserPoolCustomMessageResponse"
  }
}

sealed abstract class CognitoUserPoolDefineAuthChallengeEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolDefineAuthChallengeRequest
  def response: CognitoUserPoolDefineAuthChallengeResponse
}

object CognitoUserPoolDefineAuthChallengeEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolDefineAuthChallengeRequest,
      response: CognitoUserPoolDefineAuthChallengeResponse
  ): CognitoUserPoolDefineAuthChallengeEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolDefineAuthChallengeEvent] = Decoder.instance {
    i =>
      for {
        event <- CognitoUserPoolEvent.decodeEventData(i)
        request <- i.get[CognitoUserPoolDefineAuthChallengeRequest]("request")
        response <- i.get[CognitoUserPoolDefineAuthChallengeResponse]("response")
      } yield CognitoUserPoolDefineAuthChallengeEvent(
        event.version,
        event.triggerSource,
        event.region,
        event.userPoolId,
        event.userName,
        event.callerContext,
        request,
        response
      )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolDefineAuthChallengeEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolDefineAuthChallengeRequest,
      response: CognitoUserPoolDefineAuthChallengeResponse
  ) extends CognitoUserPoolDefineAuthChallengeEvent {
    override def productPrefix = "CognitoUserPoolDefineAuthChallengeEvent"
  }
}

sealed abstract class CognitoUserPoolDefineAuthChallengeRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def session: List[CognitoUserPoolChallengeResult]
  def userNotFound: Boolean
}

object CognitoUserPoolDefineAuthChallengeRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      session: List[CognitoUserPoolChallengeResult],
      userNotFound: Boolean
  ): CognitoUserPoolDefineAuthChallengeRequest =
    new Impl(userAttributes, clientMetadata, session, userNotFound)

  private[events] implicit val decoder: Decoder[CognitoUserPoolDefineAuthChallengeRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        session <- i.get[Option[List[CognitoUserPoolChallengeResult]]]("session")
        userNotFound <- CognitoUserPoolEventCodecs.decodeBoolean(i, "userNotFound")
      } yield CognitoUserPoolDefineAuthChallengeRequest(
        userAttributes,
        clientMetadata,
        session.getOrElse(Nil),
        userNotFound
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      session: List[CognitoUserPoolChallengeResult],
      userNotFound: Boolean
  ) extends CognitoUserPoolDefineAuthChallengeRequest {
    override def productPrefix = "CognitoUserPoolDefineAuthChallengeRequest"
  }
}

sealed abstract class CognitoUserPoolDefineAuthChallengeResponse {
  def challengeName: Option[String]
  def issueTokens: Boolean
  def failAuthentication: Boolean
}

object CognitoUserPoolDefineAuthChallengeResponse {
  def apply(
      challengeName: Option[String],
      issueTokens: Boolean,
      failAuthentication: Boolean
  ): CognitoUserPoolDefineAuthChallengeResponse =
    new Impl(challengeName, issueTokens, failAuthentication)

  private[events] implicit val decoder: Decoder[CognitoUserPoolDefineAuthChallengeResponse] =
    Decoder.instance { i =>
      for {
        challengeName <- i.get[Option[String]]("challengeName")
        issueTokens <- CognitoUserPoolEventCodecs.decodeBoolean(i, "issueTokens")
        failAuthentication <- CognitoUserPoolEventCodecs.decodeBoolean(i, "failAuthentication")
      } yield CognitoUserPoolDefineAuthChallengeResponse(
        challengeName,
        issueTokens,
        failAuthentication)
    }

  private final case class Impl(
      challengeName: Option[String],
      issueTokens: Boolean,
      failAuthentication: Boolean
  ) extends CognitoUserPoolDefineAuthChallengeResponse {
    override def productPrefix = "CognitoUserPoolDefineAuthChallengeResponse"
  }
}

sealed abstract class CognitoUserPoolMigrateUserEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolMigrateUserRequest
  def response: CognitoUserPoolMigrateUserResponse
}

object CognitoUserPoolMigrateUserEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolMigrateUserRequest,
      response: CognitoUserPoolMigrateUserResponse
  ): CognitoUserPoolMigrateUserEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolMigrateUserEvent] = Decoder.instance { i =>
    for {
      event <- CognitoUserPoolEvent.decodeEventData(i)
      request <- i.get[CognitoUserPoolMigrateUserRequest]("request")
      response <- i.get[CognitoUserPoolMigrateUserResponse]("response")
    } yield CognitoUserPoolMigrateUserEvent(
      event.version,
      event.triggerSource,
      event.region,
      event.userPoolId,
      event.userName,
      event.callerContext,
      request,
      response
    )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolMigrateUserEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolMigrateUserRequest,
      response: CognitoUserPoolMigrateUserResponse
  ) extends CognitoUserPoolMigrateUserEvent {
    override def productPrefix = "CognitoUserPoolMigrateUserEvent"
  }
}

sealed abstract class CognitoUserPoolMigrateUserRequest {
  def userAttributes: Map[String, String]
  def userName: Option[String]
  def password: Option[String]
  def validationData: Map[String, String]
  def clientMetadata: Map[String, String]
}

object CognitoUserPoolMigrateUserRequest {
  def apply(
      userAttributes: Map[String, String],
      userName: Option[String],
      password: Option[String],
      validationData: Map[String, String],
      clientMetadata: Map[String, String]
  ): CognitoUserPoolMigrateUserRequest =
    new Impl(userAttributes, userName, password, validationData, clientMetadata)

  private[events] implicit val decoder: Decoder[CognitoUserPoolMigrateUserRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        userName <- i.get[Option[String]]("userName")
        password <- i.get[Option[String]]("password")
        validationData <- CognitoUserPoolEventCodecs.decodeMap(i, "validationData")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
      } yield CognitoUserPoolMigrateUserRequest(
        userAttributes,
        userName,
        password,
        validationData,
        clientMetadata
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      userName: Option[String],
      password: Option[String],
      validationData: Map[String, String],
      clientMetadata: Map[String, String]
  ) extends CognitoUserPoolMigrateUserRequest {
    override def productPrefix = "CognitoUserPoolMigrateUserRequest"
  }
}

sealed abstract class CognitoUserPoolMigrateUserResponse {
  def userAttributes: Map[String, String]
  def finalUserStatus: Option[String]
  def messageAction: Option[String]
  def desiredDeliveryMediums: List[String]
  def forceAliasCreation: Boolean
}

object CognitoUserPoolMigrateUserResponse {
  def apply(
      userAttributes: Map[String, String],
      finalUserStatus: Option[String],
      messageAction: Option[String],
      desiredDeliveryMediums: List[String],
      forceAliasCreation: Boolean
  ): CognitoUserPoolMigrateUserResponse =
    new Impl(
      userAttributes,
      finalUserStatus,
      messageAction,
      desiredDeliveryMediums,
      forceAliasCreation
    )

  private[events] implicit val decoder: Decoder[CognitoUserPoolMigrateUserResponse] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        finalUserStatus <- i.get[Option[String]]("finalUserStatus")
        messageAction <- i.get[Option[String]]("messageAction")
        desiredDeliveryMediums <- i.get[Option[List[String]]]("desiredDeliveryMediums")
        forceAliasCreation <- CognitoUserPoolEventCodecs.decodeBoolean(i, "forceAliasCreation")
      } yield CognitoUserPoolMigrateUserResponse(
        userAttributes,
        finalUserStatus,
        messageAction,
        desiredDeliveryMediums.getOrElse(Nil),
        forceAliasCreation
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      finalUserStatus: Option[String],
      messageAction: Option[String],
      desiredDeliveryMediums: List[String],
      forceAliasCreation: Boolean
  ) extends CognitoUserPoolMigrateUserResponse {
    override def productPrefix = "CognitoUserPoolMigrateUserResponse"
  }
}

sealed abstract class CognitoUserPoolPostAuthenticationEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolPostAuthenticationRequest
}

object CognitoUserPoolPostAuthenticationEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPostAuthenticationRequest
  ): CognitoUserPoolPostAuthenticationEvent =
    new Impl(version, triggerSource, region, userPoolId, userName, callerContext, request)

  implicit val decoder: Decoder[CognitoUserPoolPostAuthenticationEvent] = Decoder.instance {
    i =>
      for {
        event <- CognitoUserPoolEvent.decodeEventData(i)
        request <- i.get[CognitoUserPoolPostAuthenticationRequest]("request")
      } yield CognitoUserPoolPostAuthenticationEvent(
        event.version,
        event.triggerSource,
        event.region,
        event.userPoolId,
        event.userName,
        event.callerContext,
        request
      )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolPostAuthenticationEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPostAuthenticationRequest
  ) extends CognitoUserPoolPostAuthenticationEvent {
    override def productPrefix = "CognitoUserPoolPostAuthenticationEvent"
  }
}

sealed abstract class CognitoUserPoolPostAuthenticationRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def newDeviceUsed: Boolean
}

object CognitoUserPoolPostAuthenticationRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      newDeviceUsed: Boolean
  ): CognitoUserPoolPostAuthenticationRequest =
    new Impl(userAttributes, clientMetadata, newDeviceUsed)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPostAuthenticationRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        newDeviceUsed <- CognitoUserPoolEventCodecs.decodeBoolean(i, "newDeviceUsed")
      } yield CognitoUserPoolPostAuthenticationRequest(
        userAttributes,
        clientMetadata,
        newDeviceUsed)
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      newDeviceUsed: Boolean
  ) extends CognitoUserPoolPostAuthenticationRequest {
    override def productPrefix = "CognitoUserPoolPostAuthenticationRequest"
  }
}

sealed abstract class CognitoUserPoolPostConfirmationEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolPostConfirmationRequest
}

object CognitoUserPoolPostConfirmationEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPostConfirmationRequest
  ): CognitoUserPoolPostConfirmationEvent =
    new Impl(version, triggerSource, region, userPoolId, userName, callerContext, request)

  implicit val decoder: Decoder[CognitoUserPoolPostConfirmationEvent] = Decoder.instance { i =>
    for {
      event <- CognitoUserPoolEvent.decodeEventData(i)
      request <- i.get[CognitoUserPoolPostConfirmationRequest]("request")
    } yield CognitoUserPoolPostConfirmationEvent(
      event.version,
      event.triggerSource,
      event.region,
      event.userPoolId,
      event.userName,
      event.callerContext,
      request
    )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolPostConfirmationEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPostConfirmationRequest
  ) extends CognitoUserPoolPostConfirmationEvent {
    override def productPrefix = "CognitoUserPoolPostConfirmationEvent"
  }
}

sealed abstract class CognitoUserPoolPostConfirmationRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
}

object CognitoUserPoolPostConfirmationRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String]
  ): CognitoUserPoolPostConfirmationRequest =
    new Impl(userAttributes, clientMetadata)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPostConfirmationRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
      } yield CognitoUserPoolPostConfirmationRequest(userAttributes, clientMetadata)
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String]
  ) extends CognitoUserPoolPostConfirmationRequest {
    override def productPrefix = "CognitoUserPoolPostConfirmationRequest"
  }
}

sealed abstract class CognitoUserPoolPreAuthenticationEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolPreAuthenticationRequest
}

object CognitoUserPoolPreAuthenticationEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreAuthenticationRequest
  ): CognitoUserPoolPreAuthenticationEvent =
    new Impl(version, triggerSource, region, userPoolId, userName, callerContext, request)

  implicit val decoder: Decoder[CognitoUserPoolPreAuthenticationEvent] = Decoder.instance { i =>
    for {
      event <- CognitoUserPoolEvent.decodeEventData(i)
      request <- i.get[CognitoUserPoolPreAuthenticationRequest]("request")
    } yield CognitoUserPoolPreAuthenticationEvent(
      event.version,
      event.triggerSource,
      event.region,
      event.userPoolId,
      event.userName,
      event.callerContext,
      request
    )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolPreAuthenticationEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreAuthenticationRequest
  ) extends CognitoUserPoolPreAuthenticationEvent {
    override def productPrefix = "CognitoUserPoolPreAuthenticationEvent"
  }
}

sealed abstract class CognitoUserPoolPreAuthenticationRequest {
  def userAttributes: Map[String, String]
  def validationData: Map[String, String]
  def userNotFound: Boolean
}

object CognitoUserPoolPreAuthenticationRequest {
  def apply(
      userAttributes: Map[String, String],
      validationData: Map[String, String],
      userNotFound: Boolean
  ): CognitoUserPoolPreAuthenticationRequest =
    new Impl(userAttributes, validationData, userNotFound)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPreAuthenticationRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        validationData <- CognitoUserPoolEventCodecs.decodeMap(i, "validationData")
        userNotFound <- CognitoUserPoolEventCodecs.decodeBoolean(i, "userNotFound")
      } yield CognitoUserPoolPreAuthenticationRequest(
        userAttributes,
        validationData,
        userNotFound)
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      validationData: Map[String, String],
      userNotFound: Boolean
  ) extends CognitoUserPoolPreAuthenticationRequest {
    override def productPrefix = "CognitoUserPoolPreAuthenticationRequest"
  }
}

sealed abstract class CognitoUserPoolPreSignUpEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolPreSignUpRequest
  def response: CognitoUserPoolPreSignUpResponse
}

object CognitoUserPoolPreSignUpEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreSignUpRequest,
      response: CognitoUserPoolPreSignUpResponse
  ): CognitoUserPoolPreSignUpEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolPreSignUpEvent] = Decoder.instance { i =>
    for {
      event <- CognitoUserPoolEvent.decodeEventData(i)
      request <- i.get[CognitoUserPoolPreSignUpRequest]("request")
      response <- i.get[CognitoUserPoolPreSignUpResponse]("response")
    } yield CognitoUserPoolPreSignUpEvent(
      event.version,
      event.triggerSource,
      event.region,
      event.userPoolId,
      event.userName,
      event.callerContext,
      request,
      response
    )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolPreSignUpEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreSignUpRequest,
      response: CognitoUserPoolPreSignUpResponse
  ) extends CognitoUserPoolPreSignUpEvent {
    override def productPrefix = "CognitoUserPoolPreSignUpEvent"
  }
}

sealed abstract class CognitoUserPoolPreSignUpRequest {
  def userAttributes: Map[String, String]
  def validationData: Map[String, String]
  def clientMetadata: Map[String, String]
}

object CognitoUserPoolPreSignUpRequest {
  def apply(
      userAttributes: Map[String, String],
      validationData: Map[String, String],
      clientMetadata: Map[String, String]
  ): CognitoUserPoolPreSignUpRequest =
    new Impl(userAttributes, validationData, clientMetadata)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPreSignUpRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        validationData <- CognitoUserPoolEventCodecs.decodeMap(i, "validationData")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
      } yield CognitoUserPoolPreSignUpRequest(userAttributes, validationData, clientMetadata)
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      validationData: Map[String, String],
      clientMetadata: Map[String, String]
  ) extends CognitoUserPoolPreSignUpRequest {
    override def productPrefix = "CognitoUserPoolPreSignUpRequest"
  }
}

sealed abstract class CognitoUserPoolPreSignUpResponse {
  def autoConfirmUser: Boolean
  def autoVerifyPhone: Boolean
  def autoVerifyEmail: Boolean
}

object CognitoUserPoolPreSignUpResponse {
  def apply(
      autoConfirmUser: Boolean,
      autoVerifyPhone: Boolean,
      autoVerifyEmail: Boolean
  ): CognitoUserPoolPreSignUpResponse =
    new Impl(autoConfirmUser, autoVerifyPhone, autoVerifyEmail)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPreSignUpResponse] =
    Decoder.instance { i =>
      for {
        autoConfirmUser <- CognitoUserPoolEventCodecs.decodeBoolean(i, "autoConfirmUser")
        autoVerifyPhone <- CognitoUserPoolEventCodecs.decodeBoolean(i, "autoVerifyPhone")
        autoVerifyEmail <- CognitoUserPoolEventCodecs.decodeBoolean(i, "autoVerifyEmail")
      } yield CognitoUserPoolPreSignUpResponse(
        autoConfirmUser,
        autoVerifyPhone,
        autoVerifyEmail)
    }

  private final case class Impl(
      autoConfirmUser: Boolean,
      autoVerifyPhone: Boolean,
      autoVerifyEmail: Boolean
  ) extends CognitoUserPoolPreSignUpResponse {
    override def productPrefix = "CognitoUserPoolPreSignUpResponse"
  }
}

sealed abstract class CognitoUserPoolPreTokenGenerationEvent extends CognitoUserPoolEvent {
  def request: CognitoUserPoolPreTokenGenerationRequest
  def response: CognitoUserPoolPreTokenGenerationResponse
}

object CognitoUserPoolPreTokenGenerationEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreTokenGenerationRequest,
      response: CognitoUserPoolPreTokenGenerationResponse
  ): CognitoUserPoolPreTokenGenerationEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolPreTokenGenerationEvent] = Decoder.instance {
    i =>
      for {
        event <- CognitoUserPoolEvent.decodeEventData(i)
        request <- i.get[CognitoUserPoolPreTokenGenerationRequest]("request")
        response <- i.get[CognitoUserPoolPreTokenGenerationResponse]("response")
      } yield CognitoUserPoolPreTokenGenerationEvent(
        event.version,
        event.triggerSource,
        event.region,
        event.userPoolId,
        event.userName,
        event.callerContext,
        request,
        response
      )
  }

  implicit def kernelSource: KernelSource[CognitoUserPoolPreTokenGenerationEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolPreTokenGenerationRequest,
      response: CognitoUserPoolPreTokenGenerationResponse
  ) extends CognitoUserPoolPreTokenGenerationEvent {
    override def productPrefix = "CognitoUserPoolPreTokenGenerationEvent"
  }
}

sealed abstract class CognitoUserPoolPreTokenGenerationRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def groupConfiguration: Option[CognitoUserPoolGroupConfiguration]
}

object CognitoUserPoolPreTokenGenerationRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      groupConfiguration: Option[CognitoUserPoolGroupConfiguration]
  ): CognitoUserPoolPreTokenGenerationRequest =
    new Impl(userAttributes, clientMetadata, groupConfiguration)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPreTokenGenerationRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        groupConfiguration <- i.get[Option[CognitoUserPoolGroupConfiguration]](
          "groupConfiguration")
      } yield CognitoUserPoolPreTokenGenerationRequest(
        userAttributes,
        clientMetadata,
        groupConfiguration
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      groupConfiguration: Option[CognitoUserPoolGroupConfiguration]
  ) extends CognitoUserPoolPreTokenGenerationRequest {
    override def productPrefix = "CognitoUserPoolPreTokenGenerationRequest"
  }
}

sealed abstract class CognitoUserPoolGroupConfiguration {
  def groupsToOverride: List[String]
  def iamRolesToOverride: List[String]
  def preferredRole: Option[String]
}

object CognitoUserPoolGroupConfiguration {
  def apply(
      groupsToOverride: List[String],
      iamRolesToOverride: List[String],
      preferredRole: Option[String]
  ): CognitoUserPoolGroupConfiguration =
    new Impl(groupsToOverride, iamRolesToOverride, preferredRole)

  private[events] implicit val decoder: Decoder[CognitoUserPoolGroupConfiguration] =
    Decoder.instance { i =>
      for {
        groupsToOverride <- i.get[Option[List[String]]]("groupsToOverride")
        iamRolesToOverride <- i.get[Option[List[String]]]("iamRolesToOverride")
        preferredRole <- i.get[Option[String]]("preferredRole")
      } yield CognitoUserPoolGroupConfiguration(
        groupsToOverride.getOrElse(Nil),
        iamRolesToOverride.getOrElse(Nil),
        preferredRole
      )
    }

  private final case class Impl(
      groupsToOverride: List[String],
      iamRolesToOverride: List[String],
      preferredRole: Option[String]
  ) extends CognitoUserPoolGroupConfiguration {
    override def productPrefix = "CognitoUserPoolGroupConfiguration"
  }
}

sealed abstract class CognitoUserPoolPreTokenGenerationResponse {
  def claimsOverrideDetails: Option[CognitoUserPoolClaimsOverrideDetails]
}

object CognitoUserPoolPreTokenGenerationResponse {
  def apply(
      claimsOverrideDetails: Option[CognitoUserPoolClaimsOverrideDetails]
  ): CognitoUserPoolPreTokenGenerationResponse =
    new Impl(claimsOverrideDetails)

  private[events] implicit val decoder: Decoder[CognitoUserPoolPreTokenGenerationResponse] =
    Decoder.forProduct1("claimsOverrideDetails")(
      CognitoUserPoolPreTokenGenerationResponse.apply)

  private final case class Impl(
      claimsOverrideDetails: Option[CognitoUserPoolClaimsOverrideDetails]
  ) extends CognitoUserPoolPreTokenGenerationResponse {
    override def productPrefix = "CognitoUserPoolPreTokenGenerationResponse"
  }
}

sealed abstract class CognitoUserPoolClaimsOverrideDetails {
  def claimsToAddOrOverride: Map[String, String]
  def claimsToSuppress: List[String]
  def groupOverrideDetails: Option[CognitoUserPoolGroupConfiguration]
}

object CognitoUserPoolClaimsOverrideDetails {
  def apply(
      claimsToAddOrOverride: Map[String, String],
      claimsToSuppress: List[String],
      groupOverrideDetails: Option[CognitoUserPoolGroupConfiguration]
  ): CognitoUserPoolClaimsOverrideDetails =
    new Impl(claimsToAddOrOverride, claimsToSuppress, groupOverrideDetails)

  private[events] implicit val decoder: Decoder[CognitoUserPoolClaimsOverrideDetails] =
    Decoder.instance { i =>
      for {
        claimsToAddOrOverride <- CognitoUserPoolEventCodecs.decodeMap(
          i,
          "claimsToAddOrOverride")
        claimsToSuppress <- i.get[Option[List[String]]]("claimsToSuppress")
        groupOverrideDetails <- i.get[Option[CognitoUserPoolGroupConfiguration]](
          "groupOverrideDetails")
      } yield CognitoUserPoolClaimsOverrideDetails(
        claimsToAddOrOverride,
        claimsToSuppress.getOrElse(Nil),
        groupOverrideDetails
      )
    }

  private final case class Impl(
      claimsToAddOrOverride: Map[String, String],
      claimsToSuppress: List[String],
      groupOverrideDetails: Option[CognitoUserPoolGroupConfiguration]
  ) extends CognitoUserPoolClaimsOverrideDetails {
    override def productPrefix = "CognitoUserPoolClaimsOverrideDetails"
  }
}

sealed abstract class CognitoUserPoolVerifyAuthChallengeResponseEvent
    extends CognitoUserPoolEvent {
  def request: CognitoUserPoolVerifyAuthChallengeResponseRequest
  def response: CognitoUserPoolVerifyAuthChallengeResponseResponse
}

object CognitoUserPoolVerifyAuthChallengeResponseEvent {
  def apply(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolVerifyAuthChallengeResponseRequest,
      response: CognitoUserPoolVerifyAuthChallengeResponseResponse
  ): CognitoUserPoolVerifyAuthChallengeResponseEvent =
    new Impl(
      version,
      triggerSource,
      region,
      userPoolId,
      userName,
      callerContext,
      request,
      response)

  implicit val decoder: Decoder[CognitoUserPoolVerifyAuthChallengeResponseEvent] =
    Decoder.instance { i =>
      for {
        event <- CognitoUserPoolEvent.decodeEventData(i)
        request <- i.get[CognitoUserPoolVerifyAuthChallengeResponseRequest]("request")
        response <- i.get[CognitoUserPoolVerifyAuthChallengeResponseResponse]("response")
      } yield CognitoUserPoolVerifyAuthChallengeResponseEvent(
        event.version,
        event.triggerSource,
        event.region,
        event.userPoolId,
        event.userName,
        event.callerContext,
        request,
        response
      )
    }

  implicit def kernelSource: KernelSource[CognitoUserPoolVerifyAuthChallengeResponseEvent] =
    KernelSource.emptyKernelSource

  private final case class Impl(
      version: String,
      triggerSource: String,
      region: String,
      userPoolId: String,
      userName: String,
      callerContext: CognitoUserPoolCallerContext,
      request: CognitoUserPoolVerifyAuthChallengeResponseRequest,
      response: CognitoUserPoolVerifyAuthChallengeResponseResponse
  ) extends CognitoUserPoolVerifyAuthChallengeResponseEvent {
    override def productPrefix = "CognitoUserPoolVerifyAuthChallengeResponseEvent"
  }
}

sealed abstract class CognitoUserPoolVerifyAuthChallengeResponseRequest {
  def userAttributes: Map[String, String]
  def clientMetadata: Map[String, String]
  def privateChallengeParameters: Map[String, String]
  def challengeAnswer: Option[String]
  def userNotFound: Boolean
}

object CognitoUserPoolVerifyAuthChallengeResponseRequest {
  def apply(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      privateChallengeParameters: Map[String, String],
      challengeAnswer: Option[String],
      userNotFound: Boolean
  ): CognitoUserPoolVerifyAuthChallengeResponseRequest =
    new Impl(
      userAttributes,
      clientMetadata,
      privateChallengeParameters,
      challengeAnswer,
      userNotFound)

  private[events] implicit val decoder
      : Decoder[CognitoUserPoolVerifyAuthChallengeResponseRequest] =
    Decoder.instance { i =>
      for {
        userAttributes <- CognitoUserPoolEventCodecs.decodeMap(i, "userAttributes")
        clientMetadata <- CognitoUserPoolEventCodecs.decodeMap(i, "clientMetadata")
        privateChallengeParameters <- CognitoUserPoolEventCodecs.decodeMap(
          i,
          "privateChallengeParameters")
        challengeAnswer <- i.get[Option[String]]("challengeAnswer")
        userNotFound <- CognitoUserPoolEventCodecs.decodeBoolean(i, "userNotFound")
      } yield CognitoUserPoolVerifyAuthChallengeResponseRequest(
        userAttributes,
        clientMetadata,
        privateChallengeParameters,
        challengeAnswer,
        userNotFound
      )
    }

  private final case class Impl(
      userAttributes: Map[String, String],
      clientMetadata: Map[String, String],
      privateChallengeParameters: Map[String, String],
      challengeAnswer: Option[String],
      userNotFound: Boolean
  ) extends CognitoUserPoolVerifyAuthChallengeResponseRequest {
    override def productPrefix = "CognitoUserPoolVerifyAuthChallengeResponseRequest"
  }
}

sealed abstract class CognitoUserPoolVerifyAuthChallengeResponseResponse {
  def answerCorrect: Boolean
}

object CognitoUserPoolVerifyAuthChallengeResponseResponse {
  def apply(answerCorrect: Boolean): CognitoUserPoolVerifyAuthChallengeResponseResponse =
    new Impl(answerCorrect)

  private[events] implicit val decoder
      : Decoder[CognitoUserPoolVerifyAuthChallengeResponseResponse] =
    Decoder.instance(i =>
      CognitoUserPoolEventCodecs
        .decodeBoolean(i, "answerCorrect")
        .map(CognitoUserPoolVerifyAuthChallengeResponseResponse.apply))

  private final case class Impl(answerCorrect: Boolean)
      extends CognitoUserPoolVerifyAuthChallengeResponseResponse {
    override def productPrefix = "CognitoUserPoolVerifyAuthChallengeResponseResponse"
  }
}

private object CognitoUserPoolEventCodecs {
  def decodeMap(i: HCursor, field: String): Decoder.Result[Map[String, String]] =
    i.get[Option[Map[String, String]]](field).map(_.getOrElse(Map.empty))

  def decodeBoolean(i: HCursor, field: String): Decoder.Result[Boolean] =
    i.get[Option[Boolean]](field).map(_.getOrElse(false))
}
