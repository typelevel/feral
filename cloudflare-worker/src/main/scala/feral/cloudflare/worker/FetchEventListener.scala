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

package feral
package cloudflare.worker

import cats.effect.IO
import cats.effect.SyncIO
import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import io.circe.Decoder
import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.scalajs._
import org.http4s.HttpRoutes
import org.http4s.Request
import org.typelevel.vault.Key
import org.http4s.HttpVersion

trait FetchEventListener extends IOSetup {

  def routes: Resource[IO, HttpRoutes[IO]]

  final type Setup = HttpRoutes[IO]
  protected override final val setup = routes

  private def apply(event: facade.FetchEvent): Unit =
    event.waitUntil(
      Supervisor[IO]
        .use { supervisor =>
          for {
            routes <- setupMemo
            request <- fromRequest[IO](event.request)
            properties <- IO.fromEither(decodeJs[IncomingRequestCfProperties](event.request.cf))
            httpVersion <- IO.fromEither(HttpVersion.fromString(properties.httpProtocol))
            _ <- routes(request
              .withHttpVersion(httpVersion)
              .withAttribute(FetchEventContext.key, FetchEventContext(supervisor, properties)))
              .map(toResponse[IO])
              .foldF(IO.unit)(r => IO(event.respondWith(r)))
          } yield ()
        }
        .unsafeToPromise()(runtime))

  final def main(args: Array[String]): Unit =
    addEventListener[facade.FetchEvent]("fetch", apply)

}

final case class FetchEventContext(
    supervisor: Supervisor[IO],
    properties: IncomingRequestCfProperties)

object FetchEventContext {
  private[worker] val key = Key.newKey[SyncIO, FetchEventContext].unsafeRunSync()
  def apply(request: Request[IO]): IO[FetchEventContext] =
    IO.fromEither(request.attributes.lookup(key).toRight(new NoSuchElementException))
}

final case class IncomingRequestCfProperties(
    asn: String,
    colo: String,
    country: Option[String],
    httpProtocol: String,
    requestPriority: Option[String],
    tlsCipher: String,
    tlsClientAuth: Option[Json],
    tlsVersion: String,
    city: Option[String],
    continent: Option[String],
    latitude: Option[String],
    longitude: Option[String],
    postalCode: Option[String],
    metroCode: Option[String],
    region: Option[String],
    regionCode: Option[String],
    timezone: String
)

object IncomingRequestCfProperties {
  private[worker] implicit def decoder: Decoder[IncomingRequestCfProperties] = deriveDecoder
}
