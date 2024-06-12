package feral.cloudflare.worker.facade

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSName

@js.native
@JSGlobal
@nowarn
private[worker] class Headers(map: js.Dictionary[String])
    extends js.Iterable[js.Array[String]] {

  @JSName(js.Symbol.iterator)
  def jsIterator(): js.Iterator[js.Array[String]] = js.native
}
