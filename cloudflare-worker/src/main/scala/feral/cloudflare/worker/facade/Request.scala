package feral.cloudflare.worker.facade

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
@nowarn
private[worker] class Request(input: String, init: RequestInit) extends js.Object {
  def body: ReadableStream[js.typedarray.Uint8Array] = js.native
  def bodyUsed: Boolean = js.native
  def cf: IncomingRequestCfProperties = js.native
  def headers: Headers = js.native
  def method: String = js.native
  def redirect: String = js.native
  def url: String = js.native
}

private[worker] sealed trait RequestInit extends js.Any

private[worker] object RequestInit {
  def apply(
      cf: js.UndefOr[RequestInitCfProperties] = js.undefined,
      method: js.UndefOr[String] = js.undefined,
      headers: js.UndefOr[Headers] = js.undefined,
      body: js.UndefOr[ReadableStream[js.typedarray.Uint8Array]] = js.undefined,
      redirect: js.UndefOr[String] = js.undefined
  ): RequestInit =
    js.Dynamic
      .literal(
        cf = cf,
        method = method,
        headers = headers,
        body = body,
        redirect = redirect
      ).asInstanceOf[RequestInit]
}

@js.native
private[worker] trait IncomingRequestCfProperties extends js.Any
private[worker] sealed trait RequestInitCfProperties extends js.Any
