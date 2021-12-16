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

package feral.examples

import cats.effect._
import feral.lambda._
import feral.lambda.events.KinesisStreamEvent

object KinesisLambda extends IOLambda.Simple[KinesisStreamEvent, INothing] {
  def handle(event: KinesisStreamEvent, context: Context[IO], init: Init) =
    IO.println(s"Received event with ${event.records.size} records").as(None)
}
