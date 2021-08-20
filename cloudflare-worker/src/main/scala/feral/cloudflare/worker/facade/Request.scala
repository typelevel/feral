package feral.cloudflare.worker.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
private[worker] class Request extends js.Object {
  def body: ReadableStream[js.typedarray.Uint8Array] = js.native
  def bodyUsed: Boolean = js.native
  def cf: IncomingRequestCfProperties = js.native
  def headers: Headers = js.native
  def method: String = js.native
  def redirect: String = js.native
  def url: String = js.native
}

class Response extends js.Object {
  
}
