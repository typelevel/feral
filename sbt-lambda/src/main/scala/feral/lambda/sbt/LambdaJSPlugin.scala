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

package feral.lambda.sbt

import io.chrisdavenport.npmpackage.sbtplugin.NpmPackagePlugin
import io.chrisdavenport.npmpackage.sbtplugin.NpmPackagePlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.Stage
import sbt.AutoPlugin
import sbt.Keys._
import sbt.PluginTrigger
import sbt.Plugins
import sbt.Setting
import sbt.plugins.JvmPlugin

object LambdaJSPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger

  override def requires: Plugins = ScalaJSPlugin && NpmPackagePlugin && JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    libraryDependencies +=
      BuildInfo.organization %%% BuildInfo.name.drop(4) % BuildInfo.version,
    scalaJSUseMainModuleInitializer := false,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    npmPackageOutputFilename := "index.js",
    npmPackageStage := Stage.FullOpt
  )

}
