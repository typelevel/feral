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
import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import org.http4s.ContextRequest
import org.http4s.ContextRoutes
import io.circe.Json
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.scalajs._

trait FetchEventListener extends IOSetup {

  def routes: Resource[IO, ContextRoutes[FetchEventContext[IO], IO]]

  final type Setup = ContextRoutes[FetchEventContext[IO], IO]
  protected override final val setup: Resource[IO, ContextRoutes[FetchEventContext[IO], IO]] =
    routes

  private def apply(event: facade.FetchEvent): Unit =
    event.waitUntil(
      Supervisor[IO]
        .use { supervisor =>
          for {
            routes <- setupMemo
            request <- fromRequest[IO](event.request)
            properties <- IO.fromEither(decodeJs[IncomingRequestCfProperties](event.request.cf))
            _ <- routes(ContextRequest(FetchEventContext(supervisor, properties), request))
              .map(toResponse[IO])
              .foldF(IO.unit)(r => IO(event.respondWith(r)))
          } yield ()
        }
        .unsafeToPromise()(runtime))

  final def main(args: Array[String]): Unit =
    addEventListener[facade.FetchEvent]("fetch", apply)

}

final case class FetchEventContext[F[_]](
    supervisor: Supervisor[F],
    properties: IncomingRequestCfProperties)

final case class IncomingRequestCfProperties(
    asn: String,
    colo: String,
    country: Option[String],
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
