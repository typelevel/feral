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

@js.native
private[functions] trait InvocationContext extends js.Object {
  def log(args: js.Any*): Unit = js.native
  def trace(args: js.Any*): Unit = js.native
  def debug(args: js.Any*): Unit = js.native
  def info(args: js.Any*): Unit = js.native
  def warn(args: js.Any*): Unit = js.native
  def error(args: js.Any*): Unit = js.native

  def functionId: String = js.native
  def functionName: String = js.native
  def extraInputs: ExtraInputs = js.native
  def extraOutputs: ExtraOutputs = js.native
  def retryContext: js.UndefOr[RetryContext] = js.native
  def traceContext: js.UndefOr[TraceContext] = js.native
  def options: Options = js.native
}

@js.native
private[functions] trait ExtraInputs extends js.Object {
  def get(binding: js.Any): js.Any = js.native
}

@js.native
private[functions] trait ExtraOutputs extends js.Object {
  def set(binding: js.Any, value: js.Any): Unit = js.native
}

@js.native
private[functions] trait RetryContext extends js.Object {
  def retryCount: Int = js.native
  def maxRetryCount: Int = js.native
  def exception: js.UndefOr[js.Any] = js.native
}

@js.native
private[functions] trait TraceContext extends js.Object {
  def traceParent: js.UndefOr[String] = js.native
  def traceState: js.UndefOr[String] = js.native
  def attributes: js.UndefOr[js.Dictionary[js.Any]] = js.native
}

@js.native
private[functions] trait Options extends js.Object {
  def trigger: js.UndefOr[js.Any] = js.native
  def extraInputs: js.UndefOr[js.Array[js.Any]] = js.native
  def extraOutputs: js.UndefOr[js.Array[js.Any]] = js.native
  def `return`: js.UndefOr[js.Any] = js.native
}
