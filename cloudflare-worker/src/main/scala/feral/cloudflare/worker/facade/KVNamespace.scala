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

package feral.cloudflare.worker.facade

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.|

@js.native
@nowarn
private[worker] sealed trait KVNamespace extends js.Object {

  def get(key: String, options: GetOptions): js.Promise[
    String | js.Any | js.typedarray.ArrayBuffer | ReadableStream[js.typedarray.Uint8Array]]

  def getWithMetadata(key: String, `type`: String): js.Promise[KVValueWithMetadata] = js.native

  def put(
      key: String,
      value: String | ReadableStream[js.typedarray.Uint8Array] | js.typedarray.ArrayBuffer,
      options: js.UndefOr[PutOptions] = js.native): js.Promise[Unit] = js.native

  def delete(key: String): js.Promise[Unit] = js.native

  def list(options: js.UndefOr[ListOptions]): js.Promise[ListResponse]

}

@js.native
private[worker] trait GetOptions extends js.Object

private[worker] object GetOptions {
  def apply(
      `type`: js.UndefOr[String] = js.undefined,
      cacheTtl: js.UndefOr[Double] = js.undefined
  ): GetOptions =
    js.Dynamic.literal(`type` = `type`, cacheTtl = cacheTtl).asInstanceOf[GetOptions]
}

@js.native
private[worker] sealed trait KVValueWithMetadata extends js.Object {
  def value
      : String | js.Any | js.typedarray.ArrayBuffer | ReadableStream[js.typedarray.Uint8Array] =
    js.native
  def metadata: js.Any = js.native
}

@js.native
private[worker] trait PutOptions extends js.Object

private[worker] object PutOptions {
  def apply(
      expiration: js.UndefOr[Double] = js.undefined,
      expirationTtl: js.UndefOr[Double] = js.undefined,
      metadata: js.UndefOr[js.Any] = js.undefined
  ): PutOptions = js
    .Dynamic
    .literal(expiration = expiration, expirationTtl = expirationTtl, metadata = metadata)
    .asInstanceOf[PutOptions]
}

@js.native
private[worker] trait ListOptions extends js.Object

private[worker] object ListOptions {
  def apply(
      prefix: js.UndefOr[String] = js.undefined,
      limit: js.UndefOr[Int] = js.undefined,
      cursor: js.UndefOr[String] = js.undefined
  ): ListOptions = js
    .Dynamic
    .literal(
      prefix = prefix,
      limit = limit,
      cursor = cursor
    )
    .asInstanceOf[ListOptions]
}

@js.native
private[worker] sealed trait ListResponse extends js.Object {
  def keys: js.Array[Key] = js.native
  def list_complete: Boolean = js.native
  def cursor: js.UndefOr[String] = js.native
}

@js.native
private[worker] sealed trait Key extends js.Object {
  def name: String = js.native
  def expiration: js.UndefOr[Double] = js.native
  def metadata: js.UndefOr[js.Any] = js.native
}
