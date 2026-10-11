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

package feral.lambda.events

import io.circe.literal._
import munit.FunSuite

class CognitoUserPoolEventSuite extends FunSuite {

  test("decode CognitoUserPoolCreateAuthChallengeEvent") {
    createAuthChallengeEvent.as[CognitoUserPoolCreateAuthChallengeEvent].toTry.get
  }

  test("decode CognitoUserPoolCustomMessageEvent") {
    customMessageEvent.as[CognitoUserPoolCustomMessageEvent].toTry.get
  }

  test("decode CognitoUserPoolDefineAuthChallengeEvent") {
    val decoded = defineAuthChallengeEvent.as[CognitoUserPoolDefineAuthChallengeEvent].toTry.get
    assertEquals(decoded.response.issueTokens, false)
  }

  test("decode CognitoUserPoolMigrateUserEvent") {
    val decoded = migrateUserEvent.as[CognitoUserPoolMigrateUserEvent].toTry.get
    assertEquals(decoded.response.forceAliasCreation, true)
  }

  test("decode CognitoUserPoolPostAuthenticationEvent") {
    postAuthenticationEvent.as[CognitoUserPoolPostAuthenticationEvent].toTry.get
  }

  test("decode CognitoUserPoolPostConfirmationEvent") {
    postConfirmationEvent.as[CognitoUserPoolPostConfirmationEvent].toTry.get
  }

  test("decode CognitoUserPoolPreAuthenticationEvent") {
    preAuthenticationEvent.as[CognitoUserPoolPreAuthenticationEvent].toTry.get
  }

  test("decode CognitoUserPoolPreSignUpEvent") {
    val decoded = preSignUpEvent.as[CognitoUserPoolPreSignUpEvent].toTry.get
    assertEquals(decoded.response.autoConfirmUser, true)
  }

  test("decode CognitoUserPoolPreTokenGenerationEvent") {
    preTokenGenerationEvent.as[CognitoUserPoolPreTokenGenerationEvent].toTry.get
  }

  test("decode CognitoUserPoolVerifyAuthChallengeResponseEvent") {
    verifyAuthChallengeResponseEvent
      .as[CognitoUserPoolVerifyAuthChallengeResponseEvent]
      .toTry
      .get
  }

  private[this] def baseEventJson = json"""
  {
    "version": "1",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    }
  }
  """

  private[this] def session = json"""
  [
    {
      "challengeName": "CUSTOM_CHALLENGE",
      "challengeResult": true,
      "challengeMetadata": "challenge-meta"
    }
  ]
  """

  private[this] def createAuthChallengeEvent = json"""
  {
    "version": ${baseEventJson.hcursor.get[String]("version").toOption.get},
    "triggerSource": "CreateAuthChallenge_Authentication",
    "region": ${baseEventJson.hcursor.get[String]("region").toOption.get},
    "userPoolId": ${baseEventJson.hcursor.get[String]("userPoolId").toOption.get},
    "userName": ${baseEventJson.hcursor.get[String]("userName").toOption.get},
    "callerContext": ${baseEventJson.hcursor.downField("callerContext").focus.get},
    "request": {
      "userAttributes": {
        "sub": "abc",
        "email": "user@example.com"
      },
      "clientMetadata": {
        "key": "value"
      },
      "challengeName": "CUSTOM_CHALLENGE",
      "session": ${session},
      "userNotFound": false
    },
    "response": {
      "publicChallengeParameters": {
        "captchaUrl": "https://example.com"
      },
      "privateChallengeParameters": {
        "answer": "42"
      },
      "challengeMetadata": "challenge-meta"
    }
  }
  """

  private[this] def customMessageEvent = json"""
  {
    "version": "1",
    "triggerSource": "CustomMessage_SignUp",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "locale": "en-US"
      },
      "codeParameter": "{####}",
      "usernameParameter": "{username}"
    },
    "response": {
      "smsMessage": "Your code is {####}",
      "emailMessage": "Your code is {####}",
      "emailSubject": "Verification code"
    }
  }
  """

  private[this] def defineAuthChallengeEvent = json"""
  {
    "version": "1",
    "triggerSource": "DefineAuthChallenge_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "channel": "web"
      },
      "session": ${session},
      "userNotFound": false
    },
    "response": {
      "challengeName": "CUSTOM_CHALLENGE",
      "issueTokens": false,
      "failAuthentication": false
    }
  }
  """

  private[this] def migrateUserEvent = json"""
  {
    "version": "1",
    "triggerSource": "UserMigration_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "email": "user@example.com"
      },
      "userName": "some-user",
      "password": "hunter2",
      "validationData": {
        "ip": "127.0.0.1"
      },
      "clientMetadata": {
        "source": "legacy-idp"
      }
    },
    "response": {
      "userAttributes": {
        "email": "user@example.com",
        "email_verified": "true"
      },
      "finalUserStatus": "CONFIRMED",
      "messageAction": "SUPPRESS",
      "desiredDeliveryMediums": [
        "EMAIL"
      ],
      "forceAliasCreation": true
    }
  }
  """

  private[this] def postAuthenticationEvent = json"""
  {
    "version": "1",
    "triggerSource": "PostAuthentication_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "source": "web"
      },
      "newDeviceUsed": true
    }
  }
  """

  private[this] def postConfirmationEvent = json"""
  {
    "version": "1",
    "triggerSource": "PostConfirmation_ConfirmSignUp",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "campaign": "spring"
      }
    }
  }
  """

  private[this] def preAuthenticationEvent = json"""
  {
    "version": "1",
    "triggerSource": "PreAuthentication_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "validationData": {
        "risk": "low"
      },
      "userNotFound": false
    }
  }
  """

  private[this] def preSignUpEvent = json"""
  {
    "version": "1",
    "triggerSource": "PreSignUp_SignUp",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "email": "user@example.com"
      },
      "validationData": {
        "tenant": "acme"
      },
      "clientMetadata": {
        "source": "app"
      }
    },
    "response": {
      "autoConfirmUser": true,
      "autoVerifyPhone": false,
      "autoVerifyEmail": true
    }
  }
  """

  private[this] def preTokenGenerationEvent = json"""
  {
    "version": "1",
    "triggerSource": "TokenGeneration_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "source": "mobile"
      },
      "groupConfiguration": {
        "groupsToOverride": [
          "admins"
        ],
        "iamRolesToOverride": [
          "arn:aws:iam::123456789012:role/Admin"
        ],
        "preferredRole": "arn:aws:iam::123456789012:role/Admin"
      }
    },
    "response": {
      "claimsOverrideDetails": {
        "claimsToAddOrOverride": {
          "custom:tenant": "acme"
        },
        "claimsToSuppress": [
          "email"
        ],
        "groupOverrideDetails": {
          "groupsToOverride": [
            "admins"
          ],
          "iamRolesToOverride": [
            "arn:aws:iam::123456789012:role/Admin"
          ],
          "preferredRole": "arn:aws:iam::123456789012:role/Admin"
        }
      }
    }
  }
  """

  private[this] def verifyAuthChallengeResponseEvent = json"""
  {
    "version": "1",
    "triggerSource": "VerifyAuthChallengeResponse_Authentication",
    "region": "eu-west-1",
    "userPoolId": "eu-west-1_123456",
    "userName": "some-user",
    "callerContext": {
      "awsSdkVersion": "aws-sdk-unknown-unknown",
      "clientId": "example-client-id"
    },
    "request": {
      "userAttributes": {
        "sub": "abc"
      },
      "clientMetadata": {
        "origin": "app"
      },
      "privateChallengeParameters": {
        "expectedAnswer": "42"
      },
      "challengeAnswer": "42",
      "userNotFound": false
    },
    "response": {
      "answerCorrect": true
    }
  }
  """
}
