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
import com.amazonaws.services.lambda.runtime
import com.amazonaws.services.lambda.runtime.Client
import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.JsonObject
import munit.CatsEffectSuite
import munit.Compare
import munit.ScalaCheckEffectSuite
import org.scalacheck._
import org.scalacheck.effect.PropF

import java.util
import java.util.Collections
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class ContextPlatformSuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  private val genCognitoIdentity: Gen[runtime.CognitoIdentity] =
    for {
      identityId <- Gen.alphaNumStr
      identityPoolId <- Gen.alphaNumStr
    } yield new runtime.CognitoIdentity {
      override def getIdentityId: String = identityId
      override def getIdentityPoolId: String = identityPoolId

      override def toString: String =
        s"CognitoIdentity(identityId=$identityId, identityPoolId=$identityPoolId)"
    }

  private val genClient: Gen[runtime.Client] =
    for {
      installationId <- Gen.alphaNumStr
      appTitle <- Gen.alphaNumStr
      appVersionName <- Gen.alphaNumStr
      appVersionCode <- Gen.alphaNumStr
      appPackageName <- Gen.alphaNumStr
    } yield new runtime.Client {
      override def getInstallationId: String = installationId
      override def getAppTitle: String = appTitle
      override def getAppVersionName: String = appVersionName
      override def getAppVersionCode: String = appVersionCode
      override def getAppPackageName: String = appPackageName

      override def toString: String =
        s"Client(installationId=$installationId, appTitle=$appTitle, appVersionName=$appVersionName, appVersionCode=$appVersionCode, appPackageName=$appPackageName)"
    }

  private val genMapStringStringEntry: Gen[(String, String)] =
    for {
      key <- Gen.alphaNumStr
      value <- Gen.alphaNumStr
    } yield (key, value)

  private val genClientContext: Gen[runtime.ClientContext] =
    for {
      client <- Gen.option(genClient).map(_.orNull)
      custom <- Gen
        .option(Gen.mapOf[String, String](genMapStringStringEntry).map(_.asJava))
        .map(_.orNull)
      env <- Gen
        .option {
          for {
            platformVersion <- Gen.option(Gen.asciiPrintableStr.map("platformVersion" -> _))
            platform <- Gen.option(Gen.asciiPrintableStr.map("platform" -> _))
            make <- Gen.option(Gen.asciiPrintableStr.map("make" -> _))
            model <- Gen.option(Gen.asciiPrintableStr.map("model" -> _))
            locale <- Gen.option(Gen.asciiPrintableStr.map("locale" -> _))
          } yield List(platformVersion, platform, make, model, locale)
            .collect { case Some((k, v)) => k -> v }
            .toMap
            .asJava
        }
        .map(_.orNull)
    } yield new runtime.ClientContext {
      override def getClient: Client = client
      override def getCustom: util.Map[String, String] = custom
      override def getEnvironment: util.Map[String, String] = env

      override def toString: String =
        s"ClientContext(client=$client, env=$env, custom=$custom)"
    }

  private implicit val arbContext: Arbitrary[runtime.Context] = Arbitrary {
    for {
      functionName <- Gen.asciiPrintableStr
      functionVersion <- Gen.alphaNumStr
      invokedFunctionArn <- Gen.asciiPrintableStr // TODO this could be formatted as an ARN
      memoryLimitInMB <- Gen.posNum[Int]
      awsRequestId <- Gen.alphaNumStr
      logGroupName <- Gen.option(Gen.asciiPrintableStr).map(_.orNull)
      logStreamName <- Gen.option(Gen.asciiPrintableStr).map(_.orNull)
      identity <- Gen.option(genCognitoIdentity).map(_.orNull)
      clientContext <- Gen.option(genClientContext).map(_.orNull)
      remainingTimeInMillis <- Gen.posNum[Int]
      tenantId <- Gen.option(Gen.asciiPrintableStr).map(_.orNull)
      xrayTraceId <- Gen.option(Gen.asciiPrintableStr).map(_.orNull)
    } yield new runtime.Context {
      override def getAwsRequestId: String = awsRequestId
      override def getLogGroupName: String = logGroupName
      override def getLogStreamName: String = logStreamName
      override def getFunctionName: String = functionName
      override def getFunctionVersion: String = functionVersion
      override def getInvokedFunctionArn: String = invokedFunctionArn
      override def getIdentity: runtime.CognitoIdentity = identity
      override def getClientContext: runtime.ClientContext = clientContext
      override def getRemainingTimeInMillis: Int = remainingTimeInMillis
      override def getMemoryLimitInMB: Int = memoryLimitInMB
      override def getLogger: LambdaLogger = new LambdaLogger {
        override def log(message: String): Unit = ()
        override def log(message: Array[Byte]): Unit = ()
      }

      override def getTenantId: String = tenantId
      override def getXrayTraceId: String = xrayTraceId

      override def toString: String =
        s"Context(functionName=$functionName, functionVersion=$functionVersion, invokedFunctionArn=$invokedFunctionArn, memoryLimitInMB=$memoryLimitInMB, awsRequestId=$awsRequestId, logGroupName=$logGroupName, logStreamName=$logStreamName, identity=$identity, clientContext=$clientContext, remainingTimeInMillis=$remainingTimeInMillis, tenantId=$tenantId, xrayTraceId=$xrayTraceId)"
    }
  }

  private implicit def optionCompare[A, B](
      implicit C: Compare[A, B]): Compare[Option[A], Option[B]] =
    new Compare[Option[A], Option[B]] {
      override def isEqual(obtained: Option[A], expected: Option[B]): Boolean =
        (obtained, expected).mapN(C.isEqual).getOrElse(obtained.isEmpty && expected.isEmpty)
    }

  private implicit val compareCognitoIdentity
      : Compare[CognitoIdentity, runtime.CognitoIdentity] =
    new Compare[CognitoIdentity, runtime.CognitoIdentity] {
      override def isEqual(
          obtained: CognitoIdentity,
          expected: runtime.CognitoIdentity): Boolean =
        obtained.identityId == expected.getIdentityId && obtained.identityPoolId == expected.getIdentityPoolId
    }

  private implicit val compareClientContextClient
      : Compare[ClientContextClient, runtime.Client] =
    new Compare[ClientContextClient, runtime.Client] {
      override def isEqual(obtained: ClientContextClient, expected: runtime.Client): Boolean =
        obtained.installationId == expected.getInstallationId &&
          obtained.appTitle == expected.getAppTitle &&
          obtained.appVersionName == expected.getAppVersionName &&
          obtained.appVersionCode == expected.getAppVersionCode &&
          obtained.appPackageName == expected.getAppPackageName
    }

  private implicit val compareClientContextEnv
      : Compare[ClientContextEnv, util.Map[String, String]] =
    new Compare[ClientContextEnv, util.Map[String, String]] {
      override def isEqual(
          obtained: ClientContextEnv,
          expected: util.Map[String, String]): Boolean =
        Option(expected)
          .map(_.asScala)
          .map(_.withDefaultValue(null))
          .map { env =>
            obtained.platformVersion == env("platformVersion") &&
            obtained.platform == env("platform") &&
            obtained.make == env("make") &&
            obtained.model == env("model") &&
            obtained.locale == env("locale")
          }
          .getOrElse(
            ClientContextEnv(null, null, null, null, null) == obtained && null == expected)
    }

  private implicit val compareJsonObjectWithJavaMap
      : Compare[JsonObject, util.Map[String, String]] =
    new Compare[JsonObject, util.Map[String, String]] {
      override def isEqual(obtained: JsonObject, expected: util.Map[String, String]): Boolean =
        obtained
          .toList
          .traverse {
            case (k, v) =>
              v.as[String].tupleLeft(k)
          }
          .map { // does the Map[String, String] contain all the keys and values from the JsonObject?
            _.forall {
              case (k, v) =>
                Option(expected).getOrElse(Collections.emptyMap).get(k) == v
            }
          }
          .map { // does the JsonObject contain all the keys from the Map[String, String]?
            _ && Option(expected)
              .getOrElse(Collections.emptyMap)
              .keySet()
              .asScala
              .forall(obtained.contains)
          }
          .getOrElse(false)
    }

  private implicit val compareClientContext: Compare[ClientContext, runtime.ClientContext] =
    new Compare[ClientContext, runtime.ClientContext] {
      override def isEqual(obtained: ClientContext, expected: runtime.ClientContext): Boolean =
        (obtained.maybeClient, Option(expected.getClient))
          .mapN(implicitly[Compare[ClientContextClient, runtime.Client]].isEqual)
          .getOrElse(obtained.maybeClient.isEmpty && expected.getClient == null) &&
          implicitly[Compare[ClientContextEnv, util.Map[String, String]]]
            .isEqual(obtained.env, expected.getEnvironment) &&
          implicitly[Compare[JsonObject, util.Map[String, String]]]
            .isEqual(obtained.custom, expected.getCustom)
    }

  test("Java Context can be decoded") {
    Prop.forAll { (context: runtime.Context) =>
      val output: Context[IO] = Context.fromJava[IO](context)

      assertEquals(output.functionName, context.getFunctionName)
      assertEquals(output.functionVersion, context.getFunctionVersion)
      assertEquals(output.invokedFunctionArn, context.getInvokedFunctionArn)
      assertEquals(output.memoryLimitInMB, context.getMemoryLimitInMB)
      assertEquals(output.awsRequestId, context.getAwsRequestId)
      assertEquals(output.logGroupName, context.getLogGroupName)
      assertEquals(output.logStreamName, context.getLogStreamName)
      assertEquals(output.identity, Option(context.getIdentity))
      assertEquals(output.clientContext, Option(context.getClientContext))
    }
  }

  test("remainingTime") {
    PropF.forAllF { (context: runtime.Context) =>
      val output: Context[IO] = Context.fromJava[IO](context)

      assertIO(output.remainingTime, context.getRemainingTimeInMillis.millis)
    }
  }

}
