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

package feral.functions.facade

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import fs2.Stream
import fs2.Chunk

import cats.effect.kernel.Async
import cats.syntax.all._

@js.native
private[functions] trait JSRequest extends js.Object {
  def method: String = js.native
  def url: String = js.native
  def headers: JSHeaders = js.native
  def body: js.UndefOr[JSReadableStream] = js.native
}

@js.native
private[functions] trait JSHeaders extends js.Object {
  def get(name: String): js.UndefOr[String] = js.native
  def keys(): js.Iterator[String] = js.native
}

@js.native
private[functions] trait JSReadableStream extends js.Object {
  def getReader(): JSReadableStreamDefaultReader = js.native
}

@js.native
private[functions] trait JSReadableStreamDefaultReader extends js.Object {
  def read(): js.Promise[JSReadObject] = js.native
  def releaseLock(): Unit = js.native
  def cancel(reason: js.UndefOr[js.Any]): js.Promise[Unit]
}

@js.native
private[functions] trait JSReadObject extends js.Object {
  def value: js.UndefOr[Uint8Array] = js.native
  def done: Boolean = js.native
}

object JSHeaders {
  private[functions] def keyList(h: JSHeaders): List[String] = {
    val builder = List.newBuilder[String]
    val itr = h.keys()
    var entity = itr.next()

    while (!entity.done) {
      builder.addOne(entity.value)
      entity = itr.next()
    }

    builder.result()
  }

  object Syntax {
    // syntax for method like calls???
  }
}

object JSReadableStream {
  private[functions] def toFs2[F[_]: Async](
      streamOption: js.UndefOr[JSReadableStream]): Stream[F, Byte] = {
    streamOption.toOption match {
      case None => Stream.empty
      case Some(null) => Stream.empty
      case Some(stream) => {
        Stream.eval(Async[F].delay(stream.getReader())).flatMap { reader =>
          def nextChunk = {
            Async[F].fromPromise(Async[F].delay(reader.read())).map { read =>
              if (read.done) {
                None
              } else {
                val chunk = read
                  .value
                  .toOption
                  .map(arr => Chunk.array[Byte](toByteArray(arr)))
                  .getOrElse(Chunk.empty[Byte])

                Some(chunk)
              }
            }
          }

          Stream
            .repeatEval(nextChunk)
            .unNoneTerminate
            .flatMap(Stream.chunk)
            .onFinalize(Async[F].delay(reader.releaseLock()))
        }
      }
    }
  }

  private def toByteArray(array: Uint8Array): Array[Byte] = {
    val builder = Array.newBuilder[Byte]
    val length = array.length
    var index = 0

    while (index != length) {
      builder.addOne(array(index).toByte)
      index = index + 1
    }

    builder.result()
  }

  object Syntax {}
}
