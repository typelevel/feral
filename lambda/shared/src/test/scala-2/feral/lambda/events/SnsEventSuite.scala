package feral.lambda.events

import io.circe.Json
import io.circe.literal._
import munit.FunSuite

class SnsEventSuite extends FunSuite {
  import SnsEventSuite._

  test("decoder") {
    event.as[SnsEvent].toTry.get
  }
}

object SnsEventSuite {
  val event: Json = json"""
    {
      "Records": [
        {
          "EventVersion": "1.0",
          "EventSubscriptionArn": "arn:aws:sns:TEST",
          "EventSource": "aws:sns",
          "Sns": {
            "Signature": "TEST",
            "MessageId": "9f0cfa95-e344-4f93-89ab-5e979503ed1f",
            "Type": "Notification",
            "TopicArn": "arn:aws:sns:TEST",
            "MessageAttributes": {
              "string": {
                "Type": "String",
                "Value": "Testing"
              },
              "binary": {
                "Type": "Binary",
                "Value": "Testing"
              },
              "number": {
                "Type": "Number",
                "Value": 1e6
              },
              "numberString": {
                "Type": "Number",
                "Value": "5"
              },
              "array": {
                "Type": "String.Array",
                "Value": "[1, \"two\", true, false]"
              },
              "unsupported": {
                "Type": "FancyNewType",
                "Value": "SpecialValueHere"
              }
            },
            "SignatureVersion": "1",
            "Timestamp": "2022-04-06T01:02:03.456Z",
            "SigningCertUrl": "TEST",
            "Message": "Testing Message",
            "UnsubscribeUrl": "TEST",
            "Subject": "Test Message"
          }
        }
      ]
    }
  """
}
