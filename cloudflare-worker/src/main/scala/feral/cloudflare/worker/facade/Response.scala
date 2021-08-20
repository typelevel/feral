package feral.cloudflare.worker.facade

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
@nowarn
private[worker] class Response(
    body: js.UndefOr[ReadableStream[js.typedarray.Uint8Array]] = js.undefined,
    init: js.UndefOr[ResponseInit] = js.undefined)
    extends js.Object {
  def body: ReadableStream[js.typedarray.Uint8Array] = js.native
  def bodyUsed: Boolean = js.native
  def encodeBody: String = js.native
  def headers: Headers = js.native
  def ok: Boolean = js.native
  def redirected: Boolean = js.native
  def status: Int = js.native
  def statusText: String = js.native
  def url: String = js.native
}

@js.native
private[worker] sealed trait ResponseInit extends js.Any

private[worker] object ResponseInit {
  def apply(
      status: js.UndefOr[Int] = js.undefined,
      statusText: js.UndefOr[String] = js.undefined,
      headers: js.UndefOr[Headers] = js.undefined
  ): ResponseInit =
    js.Dynamic
      .literal(status = status, statusText = statusText, headers = headers)
      .asInstanceOf[ResponseInit]
}
