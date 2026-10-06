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

package feral.examples

import cats.effect.IO
import cats.effect.Resource
import feral.googlecloud._
import feral.googlecloud.events._
import feral.lambda.INothing
import io.circe.Decoder

import java.util.logging.Logger

import SubscribeToTopic._

object SubscribeToTopic {

  sealed abstract class PubSubBody {
    def getMessage: PubsubMessage
  }

  object PubSubBody {
    def apply(message: PubsubMessage): PubSubBody = new Impl(message)

    implicit def decoder: Decoder[PubSubBody] = Decoder.forProduct1("message")(Impl.apply)

    private case class Impl(
        getMessage: PubsubMessage
    ) extends PubSubBody {
      override def productPrefix: String = "PubSubBody"
    }

  }

}

class SubscribeToTopic extends IOCloudEventsFunction[PubSubBody, INothing] {
  val logger = Logger.getLogger(this.getClass.getName())

  def handler: Resource[IO, ContextEventWithData[PubSubBody] => IO[Unit]] = {
    Resource.pure { event =>
      val msg = event.data.getMessage

      IO.pure {
        logger.info(s"Message ID: ${msg.messageId}")
        logger.info(s"Publish Time: ${msg.publishTime}")
        logger.info(s"Attributes: ${msg.attributes.mkString(", ")}")
        logger.info(s"data: ${msg.data.toBase64}")
      } >> IO.println("done")
    }
  }

}
