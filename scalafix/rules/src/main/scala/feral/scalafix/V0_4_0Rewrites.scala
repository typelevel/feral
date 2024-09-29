/*
 * Copyright 2023 Typelevel
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

package feral.scalafix

import scalafix.v1._

class V0_4_0Rewrites extends SemanticRule("V0_4_0Rewrites") {
  override def fix(implicit doc: SemanticDocument): Patch =
    Patch.replaceSymbols(
      "feral.lambda.AwsTags" -> "feral.lambda.natchez.AwsTags",
      "feral.lambda.KernelSource" -> "feral.lambda.natchez.KernelSource",
      "feral.lambda.TracedHandler" -> "feral.lambda.natchez.TracedHandler",
    )
}
