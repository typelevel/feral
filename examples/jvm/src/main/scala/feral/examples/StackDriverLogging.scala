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
import feral.lambda.INothing

import java.util.Base64
import java.util.logging.Logger

import SubscribeToTopic._

class StackDriverLogging extends IOCloudEventsFunction[PubSubBody, INothing] {

  def handler: Resource[IO, ContextEventWithData[PubSubBody] => IO[Unit]] = {
    val logger = Logger.getLogger(this.getClass.getName())

    Resource.pure { event =>
      val msg = event.data.getMessage.data

      val er = msg.toBase64

      if (!er.nonEmpty) {
        logger.info("Hello World")
        IO.unit
      } else {
        val decodedMessage = Base64.getDecoder().decode(er)
        val result = new String(decodedMessage)

        val output_message = s"Hello, $result"

        IO.pure {
          logger.info(s"data over the wire: ${output_message}")
        } >> IO.println("done")
      }

    }
  }
}
