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

import cats.syntax.option._
import io.circe.literal._
import munit.FunSuite
import java.nio.charset.StandardCharsets
import scodec.bits.ByteVector

class DynamoDbStreamEventSuite extends FunSuite {

  test("decoder") {
    event.as[DynamoDbStreamEvent].toTry.get
  }

  test("AttributeValue should decode a B") {

    val vector = utf8Bytes"foo"

    val source = json"""
      {
        "B": ${vector.toBase64}
      }
    """

    val decoded = source.as[AttributeValue].toOption.flatMap(_.b)

    assertEquals(decoded, vector.some)
  }

  test("AttributeValue should decode a BS") {

    val vector = utf8Bytes"foo"

    val source = json"""
      {
        "BS": [
          ${vector.toBase64}
        ]
      }
    """

    val decoded = source.as[AttributeValue].toOption.flatMap(_.bs)

    assertEquals(decoded, List(vector).some)
  }

  def event = json"""
  {
    "Records": [
      {
        "eventID": "1",
        "eventVersion": "1.0",
        "dynamodb": {
          "Keys": {
            "Id": {
              "N": "101"
            }
          },
          "NewImage": {
            "Message": {
              "S": "New item!"
            },
            "Id": {
              "N": "101"
            },
            "Image": {
              "B": "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII"
            },
            "Images": {
              "BS": [
                "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mP8z8BQz0AEYBxVSF+FABJADveWkH6oAAAAAElFTkSuQmCC",
                "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mNk+M9Qz0AEYBxVSF+FAAhKDveksOjmAAAAAElFTkSuQmCC",
                "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mNkYPhfz0AEYBxVSF+FAP5FDvcfRYWgAAAAAElFTkSuQmCC"
              ]
            }
          },
          "StreamViewType": "NEW_AND_OLD_IMAGES",
          "SequenceNumber": "111",
          "SizeBytes": 26
        },
        "awsRegion": "us-west-2",
        "eventName": "INSERT",
        "eventSourceARN": "arn:aws:dynamodb:us-west-2:account-id:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899",
        "eventSource": "aws:dynamodb"
      },
      {
        "eventID": "2",
        "eventVersion": "1.0",
        "dynamodb": {
          "OldImage": {
            "Message": {
              "S": "New item!"
            },
            "Id": {
              "N": "101"
            }
          },
          "SequenceNumber": "222",
          "Keys": {
            "Id": {
              "N": "101"
            }
          },
          "SizeBytes": 59,
          "NewImage": {
            "Message": {
              "S": "This item has changed"
            },
            "Id": {
              "N": "101"
            }
          },
          "StreamViewType": "NEW_AND_OLD_IMAGES"
        },
        "awsRegion": "us-west-2",
        "eventName": "MODIFY",
        "eventSourceARN": "arn:aws:dynamodb:us-west-2:account-id:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899",
        "eventSource": "aws:dynamodb"
      },
      {
        "eventID": "3",
        "eventVersion": "1.0",
        "dynamodb": {
          "Keys": {
            "Id": {
              "N": "101"
            }
          },
          "SizeBytes": 38,
          "SequenceNumber": "333",
          "OldImage": {
            "Message": {
              "S": "This item has changed"
            },
            "Id": {
              "N": "101"
            }
          },
          "StreamViewType": "NEW_AND_OLD_IMAGES"
        },
        "awsRegion": "us-west-2",
        "eventName": "REMOVE",
        "eventSourceARN": "arn:aws:dynamodb:us-west-2:account-id:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899",
        "eventSource": "aws:dynamodb"
      }
    ]
  }
  """

}
