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

import cats.effect._
import cats.syntax.all._
import io.circe.JsonObject
import io.circe.jawn
import munit.CatsEffectSuite
import munit.Compare
import munit.ScalaCheckEffectSuite
import org.scalacheck._
import org.scalacheck.effect.PropF

import scala.concurrent.duration._
import scala.scalajs._
import scala.scalajs.js.JSConverters._

class ContextPlatformSuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  override def scalaCheckInitialSeed = "Knv3rf_mveQ7dlnyIzRvKi0xdjT6D--8sOcl_v2-1uH="

  private val genCognitoIdentity: Gen[facade.CognitoIdentity] =
    for {
      identityId <- Gen.alphaNumStr
      identityPoolId <- Gen.alphaNumStr
    } yield new facade.CognitoIdentity {
      override def cognitoIdentityId: String = identityId
      override def cognitoIdentityPoolId: String = identityPoolId

      override def toString(): String =
        s"CognitoIdentity(identityId=$identityId, identityPoolId=$identityPoolId)"
    }

  private val genClient: Gen[facade.ClientContextClient] =
    for {
      generatedInstallationId <- Gen.alphaNumStr
      generatedAppTitle <- Gen.alphaNumStr
      generatedAppVersionName <- Gen.alphaNumStr
      generatedAppVersionCode <- Gen.alphaNumStr
      generatedAppPackageName <- Gen.alphaNumStr
    } yield new facade.ClientContextClient {
      override def installationId: String = generatedInstallationId
      override def appTitle: String = generatedAppTitle
      override def appVersionName: String = generatedAppVersionName
      override def appVersionCode: String = generatedAppVersionCode
      override def appPackageName: String = generatedAppPackageName

      override def toString(): String =
        s"Client(installationId=$installationId, appTitle=$appTitle, appVersionName=$appVersionName, appVersionCode=$appVersionCode, appPackageName=$appPackageName)"
    }

  private val genMapStringStringEntry: Gen[(String, String)] =
    for {
      key <- Gen.alphaNumStr
      value <- Gen.alphaNumStr
    } yield (key, value)

  private val genClientContext: Gen[facade.ClientContext] =
    for {
      generatedClient <- Gen.option(genClient).map(_.orUndefined)
      generatedCustom <- Gen
        .option(Gen.mapOf[String, String](genMapStringStringEntry).map { m =>
          val dict = js.Dynamic.literal()
          m.foreach { case (k, v) => dict.updateDynamic(k)(v) }
          dict
        })
        .map(_.orUndefined)
      generatedEnv <- { // TODO this should allow for undefined values but the types don't line up
        for {
          generatedPlatformVersion <- Gen.asciiPrintableStr
          generatedPlatform <- Gen.asciiPrintableStr
          generatedMake <- Gen.asciiPrintableStr
          generatedModel <- Gen.asciiPrintableStr
          generatedLocale <- Gen.asciiPrintableStr
        } yield new facade.ClientContextEnv {
          override def platformVersion: String = generatedPlatformVersion
          override def platform: String = generatedPlatform
          override def make: String = generatedMake
          override def model: String = generatedModel
          override def locale: String = generatedLocale
        }
      }
    } yield new facade.ClientContext {
      override def client: js.UndefOr[facade.ClientContextClient] = generatedClient
      override def custom: js.UndefOr[js.Any] = generatedCustom
      override def env: facade.ClientContextEnv = generatedEnv

      override def toString(): String =
        s"ClientContext(client=$client, env=$env, custom=$custom)"
    }

  private implicit val arbContext: Arbitrary[facade.Context] = Arbitrary {
    for {
      generatedFunctionName <- Gen.asciiPrintableStr
      generatedFunctionVersion <- Gen.alphaNumStr
      generatedInvokedFunctionArn <-
        Gen.asciiPrintableStr // TODO this could be formatted as an ARN
      generatedMemoryLimitInMB <- Gen.posNum[Int].map(_.toString)
      generatedAwsRequestId <- Gen.alphaNumStr
      generatedLogGroupName <- Gen.asciiPrintableStr
      generatedLogStreamName <- Gen.asciiPrintableStr
      generatedIdentity <- Gen.option(genCognitoIdentity).map(_.orUndefined)
      generatedClientContext <- Gen.option(genClientContext).map(_.orUndefined)
      generatedRemainingTimeInMillis <- Gen.posNum[Double]
    } yield new facade.Context {
      override def functionName: String = generatedFunctionName
      override def functionVersion: String = generatedFunctionVersion
      override def invokedFunctionArn: String = generatedInvokedFunctionArn
      override def memoryLimitInMB: String = generatedMemoryLimitInMB
      override def awsRequestId: String = generatedAwsRequestId
      override def logGroupName: String = generatedLogGroupName
      override def logStreamName: String = generatedLogStreamName
      override def identity: js.UndefOr[facade.CognitoIdentity] = generatedIdentity
      override def clientContext: js.UndefOr[facade.ClientContext] = generatedClientContext
      override def getRemainingTimeInMillis(): Double = generatedRemainingTimeInMillis

      override def toString(): String =
        s"Context(functionName=$functionName, functionVersion=$functionVersion, invokedFunctionArn=$invokedFunctionArn, memoryLimitInMB=$memoryLimitInMB, awsRequestId=$awsRequestId, logGroupName=$logGroupName, logStreamName=$logStreamName, identity=$identity, clientContext=$clientContext, remainingTimeInMillis=${getRemainingTimeInMillis()}"
    }
  }

  private implicit def optionCompare[A, B](
      implicit C: Compare[A, B]): Compare[Option[A], Option[B]] =
    new Compare[Option[A], Option[B]] {
      override def isEqual(obtained: Option[A], expected: Option[B]): Boolean =
        (obtained, expected).mapN(C.isEqual).getOrElse(obtained.isEmpty && expected.isEmpty)
    }

  private implicit val compareCognitoIdentity
      : Compare[CognitoIdentity, facade.CognitoIdentity] =
    new Compare[CognitoIdentity, facade.CognitoIdentity] {
      override def isEqual(
          obtained: CognitoIdentity,
          expected: facade.CognitoIdentity): Boolean =
        obtained.identityId == expected.cognitoIdentityId && obtained.identityPoolId == expected.cognitoIdentityPoolId
    }

  private implicit val compareClientContextClient
      : Compare[ClientContextClient, facade.ClientContextClient] =
    new Compare[ClientContextClient, facade.ClientContextClient] {
      override def isEqual(
          obtained: ClientContextClient,
          expected: facade.ClientContextClient): Boolean =
        obtained.installationId == expected.installationId &&
          obtained.appTitle == expected.appTitle &&
          obtained.appVersionName == expected.appVersionName &&
          obtained.appVersionCode == expected.appVersionCode &&
          obtained.appPackageName == expected.appPackageName
    }

  private implicit val compareClientContextEnv
      : Compare[ClientContextEnv, facade.ClientContextEnv] =
    new Compare[ClientContextEnv, facade.ClientContextEnv] {
      override def isEqual(
          obtained: ClientContextEnv,
          expected: facade.ClientContextEnv): Boolean =
        obtained.platformVersion == expected.platformVersion &&
          obtained.platform == expected.platform &&
          obtained.make == expected.make &&
          obtained.model == expected.model &&
          obtained.locale == expected.locale
    }

  private implicit val compareJsonObjectWithUndefOrAny
      : Compare[JsonObject, js.UndefOr[js.Any]] = new Compare[JsonObject, js.UndefOr[js.Any]] {
    override def isEqual(obtained: JsonObject, expected: js.UndefOr[js.Any]): Boolean =
      expected
        .toOption
        .map(js.JSON.stringify(_))
        .flatMap(jawn.parse(_).toOption)
        .flatMap(_.asObject)
        .map(_.equals(obtained))
        .getOrElse(obtained.isEmpty && expected.isEmpty)
  }

  private implicit val compareClientContext: Compare[ClientContext, facade.ClientContext] =
    new Compare[ClientContext, facade.ClientContext] {
      override def isEqual(obtained: ClientContext, expected: facade.ClientContext): Boolean =
        (obtained.maybeClient, expected.client.toOption)
          .mapN(implicitly[Compare[ClientContextClient, facade.ClientContextClient]].isEqual)
          .getOrElse(obtained.maybeClient.isEmpty && expected.client.isEmpty) &&
          implicitly[Compare[ClientContextEnv, facade.ClientContextEnv]]
            .isEqual(obtained.env, expected.env) &&
          implicitly[Compare[JsonObject, js.UndefOr[js.Any]]]
            .isEqual(obtained.custom, expected.custom)
    }

  test("JS Context can be decoded") {
    Prop.forAll { (context: facade.Context) =>
      val output: Context[IO] = Context.fromJS[IO](context)

      assertEquals(output.functionName, context.functionName)
      assertEquals(output.functionVersion, context.functionVersion)
      assertEquals(output.invokedFunctionArn, context.invokedFunctionArn)
      assertEquals(output.memoryLimitInMB.toString, context.memoryLimitInMB)
      assertEquals(output.awsRequestId, context.awsRequestId)
      assertEquals(output.logGroupName, context.logGroupName)
      assertEquals(output.logStreamName, context.logStreamName)
      assertEquals(output.identity, context.identity.toOption)
      assertEquals(output.clientContext, context.clientContext.toOption)
    }
  }

  test("remainingTime") {
    PropF.forAllF { (context: facade.Context) =>
      val output: Context[IO] = Context.fromJS[IO](context)

      assertIO(output.remainingTime, context.getRemainingTimeInMillis().millis)
    }
  }

}
