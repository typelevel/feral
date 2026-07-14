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

package feral.cloudflare.worker

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.concurrent.duration.FiniteDuration
import io.circe.Decoder

import fs2.Stream
import scodec.bits.ByteVector
import io.circe.Encoder
import io.circe.scalajs._
import cats.effect.kernel.Async
import cats.syntax.all._
import cats.data.OptionT
import fs2.Chunk
import cats.effect.kernel.Resource

sealed trait KVNamespace[F[_]] {

  import KVNamespace._

  def getText(key: String): F[Option[String]]
  def getText(key: String, cacheTtl: FiniteDuration): F[Option[String]]

  def get[A: Decoder](key: String): F[Option[A]]
  def get[A: Decoder](key: String, cacheTtl: FiniteDuration): F[Option[A]]

  def getByteVector(key: String): F[Option[ByteVector]]
  def getByteVector(key: String, cacheTtl: FiniteDuration): F[Option[ByteVector]]

  def getStream(key: String): Resource[F, Option[Stream[F, Byte]]]
  def getStream(key: String, cacheTtl: FiniteDuration): Resource[F, Option[Stream[F, Byte]]]

  def getTextWithMetadata[M: Decoder](key: String): F[Option[(String, Option[M])]]

  def getWithMetadata[A: Decoder, M: Decoder](key: String): F[Option[(A, Option[M])]]

  def getByteVectorWithMetadata[M: Decoder](key: String): F[Option[(ByteVector, Option[M])]]

  def getStreamWithMetadata[M: Decoder](
      key: String): Resource[F, Option[(Stream[F, Byte], Option[M])]]

  def put(key: String, value: String): F[Unit] = put[Unit](key, value, PutOptions())
  def put[M: Encoder](key: String, value: String, options: PutOptions[M]): F[Unit]

  def put[A: Encoder](key: String, value: A): F[Unit] = put[A, Unit](key, value, PutOptions())
  def put[A: Encoder, M: Encoder](key: String, value: A, options: PutOptions[M]): F[Unit] =
    put(key, Encoder[A].apply(value).noSpaces, options)

  def put(key: String, value: ByteVector): F[Unit] = put[Unit](key, value, PutOptions())
  def put[M: Encoder](key: String, value: ByteVector, options: PutOptions[M]): F[Unit]

  def put(key: String, value: Stream[F, Byte]): F[Unit] = put[Unit](key, value, PutOptions())
  def put[M: Encoder](key: String, value: Stream[F, Byte], options: PutOptions[M]): F[Unit]

  def delete(key: String): F[Unit]

  def list[M: Decoder]: Stream[F, Key[M]]
  def list[M: Decoder](prefix: String): Stream[F, Key[M]]

}

object KVNamespace {

  final case class Key[+M](name: String, expiration: Option[js.Date], metadata: Option[M])

  final case class PutOptions[+M](
      expiration: Option[js.Date] = None,
      expirationTtl: Option[FiniteDuration] = None,
      metadata: Option[M] = None
  ) {
    private[KVNamespace] def toJS[M0 >: M](implicit e: Encoder[M0]): facade.PutOptions =
      facade.PutOptions(
        expiration.map(_.getDate() / 1000).orUndefined,
        expirationTtl.map(_.toSeconds.toDouble).orUndefined,
        metadata.map((_: M0).asJsAny).orUndefined
      )
  }

  def apply[F[_]: Async](namespace: String): KVNamespace[F] =
    new AsyncKVNamespace[F](
      js.special
        .fileLevelThis
        .asInstanceOf[js.Dynamic]
        .selectDynamic(namespace)
        .asInstanceOf[facade.KVNamespace])

  private final class AsyncKVNamespace[F[_]](kv: facade.KVNamespace)(implicit F: Async[F])
      extends KVNamespace[F] {

    def getText(key: String): F[Option[String]] =
      F.fromPromise(F.delay(kv.get(key, facade.GetOptions("text"))))
        .map(x => Option(x.asInstanceOf[String]))

    def getText(key: String, cacheTtl: FiniteDuration): F[Option[String]] =
      F.fromPromise(
        F.delay(kv.get(key, facade.GetOptions("text", cacheTtl.toSeconds.toDouble))))
        .map(x => Option(x.asInstanceOf[String]))

    def get[A: Decoder](key: String): F[Option[A]] =
      OptionT(
        F.fromPromise(F.delay(kv.get(key, facade.GetOptions("json"))))
          .map(x => Option(x.asInstanceOf[js.Any]))
      ).semiflatMap(x => F.fromEither(decodeJs[A](x))).value

    def get[A: Decoder](key: String, cacheTtl: FiniteDuration): F[Option[A]] =
      OptionT(
        F.fromPromise(
          F.delay(kv.get(key, facade.GetOptions("json", cacheTtl.toSeconds.toDouble))))
          .map(x => Option(x.asInstanceOf[js.Any]))
      ).semiflatMap(x => F.fromEither(decodeJs[A](x))).value

    def getByteVector(key: String): F[Option[ByteVector]] =
      OptionT(
        F.fromPromise(F.delay(kv.get(key, facade.GetOptions("arrayBuffer"))))
          .map(x => Option(x.asInstanceOf[js.typedarray.ArrayBuffer]))
      ).map(Chunk.jsArrayBuffer(_).toByteVector).value

    def getByteVector(key: String, cacheTtl: FiniteDuration): F[Option[ByteVector]] =
      OptionT(
        F.fromPromise(
          F.delay(kv.get(key, facade.GetOptions("arrayBuffer", cacheTtl.toSeconds.toDouble))))
          .map(x => Option(x.asInstanceOf[js.typedarray.ArrayBuffer]))
      ).map(Chunk.jsArrayBuffer(_).toByteVector).value

    def getStream(key: String): Resource[F, Option[Stream[F, Byte]]] =
      OptionT(
        Resource.makeCase(F
          .fromPromise(F.delay(kv.get(key, facade.GetOptions("stream"))))
          .map(x => Option(x.asInstanceOf[facade.ReadableStream[js.typedarray.Uint8Array]]))) {
          case (Some(rs), exitCase) => closeReadableStream(rs, exitCase)
          case _ => F.unit
        }
      ).map(fromReadableStream[F]).value

    def getStream(key: String, cacheTtl: FiniteDuration): Resource[F, Option[Stream[F, Byte]]] =
      OptionT(
        Resource.makeCase(
          F.fromPromise(
            F.delay(kv.get(key, facade.GetOptions("stream", cacheTtl.toSeconds.toDouble))))
            .map(x =>
              Option(x.asInstanceOf[facade.ReadableStream[js.typedarray.Uint8Array]]))) {
          case (Some(rs), exitCase) => closeReadableStream(rs, exitCase)
          case _ => F.unit
        }
      ).map(fromReadableStream[F]).value

    def getTextWithMetadata[M: Decoder](key: String): F[Option[(String, Option[M])]] =
      F.fromPromise(
        F.delay(kv.getWithMetadata(key, "text"))
      ).flatMap { vm =>
        OptionT
          .fromOption(Option(vm.value.asInstanceOf[String]))
          .semiflatMap { value =>
            OptionT
              .fromOption(Option(vm.metadata))
              .semiflatMap(x => F.fromEither(decodeJs[M](x)))
              .value
              .map((value, _))
          }
          .value
      }

    def getWithMetadata[A: Decoder, M: Decoder](key: String): F[Option[(A, Option[M])]] =
      F.fromPromise(
        F.delay(kv.getWithMetadata(key, "json"))
      ).flatMap { vm =>
        OptionT
          .fromOption(Option(vm.value.asInstanceOf[js.Any]))
          .semiflatMap(x => F.fromEither(decodeJs[A](x)))
          .semiflatMap { value =>
            OptionT
              .fromOption(Option(vm.metadata))
              .semiflatMap(x => F.fromEither(decodeJs[M](x)))
              .value
              .map((value, _))
          }
          .value
      }

    def getByteVectorWithMetadata[M: Decoder](key: String): F[Option[(ByteVector, Option[M])]] =
      F.fromPromise(
        F.delay(kv.getWithMetadata(key, "arrayBuffer"))
      ).flatMap { vm =>
        OptionT
          .fromOption(Option(vm.value.asInstanceOf[js.typedarray.ArrayBuffer]))
          .semiflatMap { value =>
            OptionT
              .fromOption(Option(vm.metadata))
              .semiflatMap(x => F.fromEither(decodeJs[M](x)))
              .value
              .map((Chunk.jsArrayBuffer(value).toByteVector, _))
          }
          .value
      }

    def getStreamWithMetadata[M: Decoder](
        key: String): Resource[F, Option[(Stream[F, Byte], Option[M])]] =
      OptionT(
        Resource.makeCase(
          F.fromPromise(
            F.delay(kv.getWithMetadata(key, "stream"))
          ).flatMap { vm =>
            OptionT
              .fromOption(
                Option(vm.value.asInstanceOf[facade.ReadableStream[js.typedarray.Uint8Array]]))
              .semiflatMap { value =>
                OptionT
                  .fromOption(Option(vm.metadata))
                  .semiflatMap(x => F.fromEither(decodeJs[M](x)))
                  .value
                  .map((value, _))
              }
              .value
          }) {
          case (Some((rs, _)), exitCase) => closeReadableStream(rs, exitCase)
          case _ => F.unit
        }).map {
        case (rs, m) =>
          (fromReadableStream(rs), m)
      }.value

    def put[M: Encoder](key: String, value: String, options: PutOptions[M]): F[Unit] =
      F.fromPromise(F.delay(kv.put(key, value, options.toJS[M])))

    def put[M: Encoder](key: String, value: ByteVector, options: PutOptions[M]): F[Unit] =
      F.fromPromise(
        F.delay(kv.put(key, Chunk.byteVector(value).toJSArrayBuffer, options.toJS[M])))

    def put[M: Encoder](key: String, value: Stream[F, Byte], options: PutOptions[M]): F[Unit] =
      ???

    def delete(key: String): F[Unit] = F.fromPromise(F.delay(kv.delete(key)))

    def list[M: Decoder]: Stream[F, Key[M]] = list[M](js.undefined)

    def list[M: Decoder](prefix: String): Stream[F, Key[M]] = list[M](prefix)

    private def list[M: Decoder](prefix: js.UndefOr[String]): Stream[F, Key[M]] =
      Stream
        .unfoldLoopEval(js.undefined: js.UndefOr[String]) { cursor =>
          F.fromPromise(F.delay(kv.list(facade.ListOptions(prefix = prefix, cursor = cursor))))
            .map { response =>
              (response.keys, if (!response.list_complete) Some(response.cursor) else None)
            }
        }
        .flatMap { keys =>
          Stream.emits(keys.toSeq).evalMap { key =>
            OptionT
              .fromOption[F](key.metadata.toOption.flatMap(Option(_)))
              .semiflatMap(x => F.fromEither(decodeJs[M](x)))
              .value
              .map {
                Key(key.name, key.expiration.toOption.flatMap(Option(_)).map(new js.Date(_)), _)
              }
          }
        }
  }

}
