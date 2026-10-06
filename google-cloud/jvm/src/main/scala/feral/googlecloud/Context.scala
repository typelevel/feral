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

package feral.googlecloud

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe._
import io.circe.parser._
import io.cloudevents.CloudEvent

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._

final case class CloudEventContext(
    id: String,
    `type`: String,
    source: URI,
    dataContentType: Option[String],
    dataSchema: Option[URI],
    subject: Option[String],
    time: Option[OffsetDateTime],
    extensions: Map[String, String]
)

object CloudEventContext {
  def from(event: CloudEvent): CloudEventContext = {
    CloudEventContext(
      id = event.getId(),
      `type` = event.getType(),
      source = event.getSource(),
      dataContentType = Option(event.getDataContentType()),
      dataSchema = Option(event.getDataSchema()),
      subject = Option(event.getSubject()),
      time = Option(event.getTime()),
      extensions = event
        .getAttributeNames
        .asScala
        .filterNot(Set("id", "type", "source", "time", "datacontenttype", "subject"))
        .map(k => k -> event.getAttribute(k).toString)
        .toMap
    )
  }
}

final case class ContextEventWithData[A](
    context: CloudEventContext,
    data: A
)

object ContextEventWithData {

  def from[A: Decoder](event: CloudEvent): Either[Error, ContextEventWithData[A]] = {
    val context = CloudEventContext.from(event)

    Option(event.getData())
      .toRight(DecodingFailure("CloudEvent has no data", Nil))
      .flatMap { data =>
        // TODO - maybe refactor this to only use String instead of StringBuilder?
        val res = new StringBuilder()
        res.append(new String(data.toBytes(), StandardCharsets.UTF_8))
        parse(res.result()).flatMap(_.as[A])
      }
      .map(ContextEventWithData(context, _))
  }

}
