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

final case class Context(context: InvocationContext) {
  def log(m: String): Unit = context.log(m)
  def trace(m: String): Unit = context.trace(m)
  def debug(m: String): Unit = context.debug(m)
  def info(m: String): Unit = context.info(m)
  def warn(m: String): Unit = context.warn(m)
  def error(m: String): Unit = context.error(m)
}
