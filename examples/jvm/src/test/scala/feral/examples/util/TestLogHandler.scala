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

package feral.examples.util

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class TestLogHandler extends Handler {

  setLevel(Level.ALL)
  private val log_store: collection.mutable.ListBuffer[LogRecord] =
    scala.collection.mutable.ListBuffer.empty

  def getLog = log_store.result()
  def clear(): Unit = log_store.clear()
  def close(): Unit = ()
  def flush(): Unit = ()
  def publish(record: LogRecord): Unit = log_store += record
}

object TestLogHandler {
  def apply(): TestLogHandler = new TestLogHandler()
}
