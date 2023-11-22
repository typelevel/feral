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
import cats.syntax.all._
import feral.lambda.cloudformation.CloudFormationRequestType._
import io.circe.{Json, JsonObject}
import io.circe.testing.instances.{arbitraryJson, arbitraryJsonObject}
import org.http4s.{Charset => _, _}
import org.http4s.syntax.all._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import java.nio.charset.Charset
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.duration._

trait CloudFormationCustomResourceArbitraries {
  val genClientContextClient: Gen[ClientContextClient] =
    for {
      installationId <- arbitrary[String]
      appTitle <- arbitrary[String]
      appVersionName <- arbitrary[String]
      appVersionCode <- arbitrary[String]
      appPackageName <- arbitrary[String]
    } yield ClientContextClient(
      installationId,
      appTitle,
      appVersionName,
      appVersionCode,
      appPackageName)
  implicit val arbClientContextClient: Arbitrary[ClientContextClient] = Arbitrary(
    genClientContextClient)

  val genClientContextEnv: Gen[ClientContextEnv] =
    for {
      platformVersion <- arbitrary[String]
      platform <- arbitrary[String]
      make <- arbitrary[String]
      model <- arbitrary[String]
      locale <- arbitrary[String]
    } yield ClientContextEnv(platformVersion, platform, make, model, locale)
  implicit val arbClientContextEnv: Arbitrary[ClientContextEnv] = Arbitrary(genClientContextEnv)

  val genCognitoIdentity: Gen[CognitoIdentity] =
    for {
      identityId <- arbitrary[String]
      identityPoolId <- arbitrary[String]
    } yield CognitoIdentity(identityId, identityPoolId)
  implicit val arbCognitoIdentity: Arbitrary[CognitoIdentity] = Arbitrary(genCognitoIdentity)

  val genClientContext: Gen[ClientContext] =
    for {
      client <- arbitrary[ClientContextClient]
      env <- arbitrary[ClientContextEnv]
      custom <- arbitrary[JsonObject]
    } yield ClientContext(client, env, custom)
  implicit val arbClientContext: Arbitrary[ClientContext] = Arbitrary(genClientContext)

  def genContext[F[_]: Applicative]: Gen[Context[F]] =
    for {
      functionName <- arbitrary[String]
      functionVersion <- arbitrary[String]
      invokedFunctionArn <- arbitrary[String]
      memoryLimitInMB <- arbitrary[Int]
      awsRequestId <- arbitrary[String]
      logGroupName <- arbitrary[String]
      logStreamName <- arbitrary[String]
      identity <- arbitrary[Option[CognitoIdentity]]
      clientContext <- arbitrary[Option[ClientContext]]
      remainingTime <- arbitrary[FiniteDuration]
    } yield Context(
      functionName,
      functionVersion,
      invokedFunctionArn,
      memoryLimitInMB,
      awsRequestId,
      logGroupName,
      logStreamName,
      identity,
      clientContext,
      remainingTime.pure[F]
    )

  implicit def arbContext[F[_]: Applicative]: Arbitrary[Context[F]] = Arbitrary(genContext[F])

  val genCloudFormationRequestType: Gen[CloudFormationRequestType] =
    Gen.oneOf(
      Gen.const(CreateRequest),
      Gen.const(UpdateRequest),
      Gen.const(DeleteRequest),
      Gen.chooseNum(1, 100).flatMap(Gen.stringOfN(_, arbitrary[Char])).map(OtherRequestType(_))
    )
  implicit val arbCloudFormationRequestType: Arbitrary[CloudFormationRequestType] = Arbitrary(
    genCloudFormationRequestType)

  // TODO is there a more complete generator we want to use?
  val genUri: Gen[Uri] =
    uri"https://cloudformation-custom-resource-response-useast2.s3-us-east-2.amazonaws.com/arn%3Aaws%3Acloudformation%3Aus-east-2%3A123456789012%3Astack/lambda-error-processor/1134083a-2608-1e91-9897-022501a2c456%7Cprimerinvoke%7C5d478078-13e9-baf0-464a-7ef285ecc786?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1555451971&Signature=28UijZePE5I4dvukKQqM%2F9Rf1o4%3D"
  implicit val arbUri: Arbitrary[Uri] = Arbitrary(genUri)

  val genStackId: Gen[StackId] =
    for {
      // max of 217 (89 for the ARN format/UUID and up to 128 for the stack name)
      length <- Gen.chooseNum(1, 217)
      str <- Gen.stringOfN(length, arbitrary[Char])
    } yield StackId(str)
  implicit val arbStackId: Arbitrary[StackId] = Arbitrary(genStackId)

  val genRequestId: Gen[RequestId] = arbitrary[UUID].map(_.toString).map(RequestId(_))
  implicit val arbRequestId: Arbitrary[RequestId] = Arbitrary(genRequestId)

  val genResourceType: Gen[ResourceType] =
    for {
      // max of 60 alphanumeric and _@- characters, see https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/crpg-ref-requests.html
      length <- Gen.chooseNum(1, 60)
      str <- Gen.stringOfN(
        length,
        Gen.oneOf(Gen.alphaNumChar, Gen.const('_'), Gen.const('@'), Gen.const('-')))
    } yield ResourceType(str)
  implicit val arbResourceType: Arbitrary[ResourceType] = Arbitrary(genResourceType)

  val genLogicalResourceId: Gen[LogicalResourceId] = arbitrary[String].map(LogicalResourceId(_))
  implicit val arbLogicalResourceId: Arbitrary[LogicalResourceId] = Arbitrary(
    genLogicalResourceId)

  val genPhysicalResourceId: Gen[PhysicalResourceId] =
    for {
      length <- Gen.chooseNum(1, 1024)
      str <- Gen.stringOfN(length, Gen.asciiPrintableChar).map(trimStringToByteLength(1024)(_))
    } yield PhysicalResourceId.unsafeApply(str)
  implicit val arbPhysicalResourceId: Arbitrary[PhysicalResourceId] = Arbitrary(
    genPhysicalResourceId)

  @tailrec
  private def trimStringToByteLength(
      length: Int)(s: String, encoding: Charset = Charset.forName("UTF-8")): String =
    if (s.getBytes(encoding).length > length)
      trimStringToByteLength(length - 1)(s.drop(1), encoding)
    else s

  def genCloudFormationCustomResourceRequest[A: Arbitrary]
      : Gen[CloudFormationCustomResourceRequest[A]] =
    for {
      requestType <- arbitrary[CloudFormationRequestType]
      responseURL <- arbitrary[Uri]
      stackId <- arbitrary[StackId]
      requestId <- arbitrary[RequestId]
      resourceType <- arbitrary[ResourceType]
      logicalResourceId <- arbitrary[LogicalResourceId]
      physicalResourceId <- arbitrary[PhysicalResourceId]
      resourceProperties <- arbitrary[A]
      oldResourceProperties <- arbitrary[Option[JsonObject]]
    } yield CloudFormationCustomResourceRequest(
      requestType,
      responseURL,
      stackId,
      requestId,
      resourceType,
      logicalResourceId,
      if (requestType == CreateRequest) None else physicalResourceId.some,
      resourceProperties,
      oldResourceProperties
    )

  implicit def arbCloudFormationCustomResourceRequest[A: Arbitrary]
      : Arbitrary[CloudFormationCustomResourceRequest[A]] = Arbitrary(
    genCloudFormationCustomResourceRequest[A])

  def genInvocation[F[_]: Applicative, A: Arbitrary]
      : Gen[Invocation[F, CloudFormationCustomResourceRequest[A]]] =
    for {
      e <- arbitrary[CloudFormationCustomResourceRequest[A]]
      c <- arbitrary[Context[F]]
    } yield Invocation.pure(e, c)

  implicit def arbInvocation[F[_]: Applicative, A: Arbitrary]
      : Arbitrary[Invocation[F, CloudFormationCustomResourceRequest[A]]] =
    Arbitrary(genInvocation[F, A])

  def genHandlerResponse[A: Arbitrary]: Gen[HandlerResponse[A]] =
    for {
      physicalId <- arbitrary[PhysicalResourceId]
      a <- arbitrary[Option[A]]
    } yield HandlerResponse(physicalId, a)
  implicit def arbHandlerResponse[A: Arbitrary]: Arbitrary[HandlerResponse[A]] = Arbitrary(
    genHandlerResponse[A])

  val genRequestResponseStatus: Gen[RequestResponseStatus] =
    Gen.oneOf(RequestResponseStatus.Success, RequestResponseStatus.Failed)
  implicit val arbRequestResponseStatus: Arbitrary[RequestResponseStatus] = Arbitrary(
    genRequestResponseStatus)

  val genCloudFormationCustomResourceResponse: Gen[CloudFormationCustomResourceResponse] =
    for {
      status <- arbitrary[RequestResponseStatus]
      reason <- arbitrary[Option[String]]
      physicalResourceId <- arbitrary[Option[PhysicalResourceId]]
      stackId <- arbitrary[StackId]
      requestId <- arbitrary[RequestId]
      logicalResourceId <- arbitrary[LogicalResourceId]
      data <- arbitrary[Json]
    } yield CloudFormationCustomResourceResponse(
      status,
      reason,
      physicalResourceId,
      stackId,
      requestId,
      logicalResourceId,
      data)
  implicit val arbCloudFormationCustomResourceResponse
      : Arbitrary[CloudFormationCustomResourceResponse] = Arbitrary(
    genCloudFormationCustomResourceResponse)
}
