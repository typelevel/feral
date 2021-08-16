/*
 * Copyright 2019 Arman Bilge
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

name := "feral"

ThisBuild / baseVersion := "0.1"

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"
ThisBuild / publishGithubUser := "armanbilge"
ThisBuild / publishFullName := "Arman Bilge"

ThisBuild / crossScalaVersions := Seq("3.0.1", "2.12.14", "2.13.6")

val catsEffectVersion = "3.2.3"
val circeVersion = "0.14.1"

lazy val root =
  project.in(file(".")).aggregate(lambda.js, lambda.jvm).enablePlugins(NoPublishPlugin)

lazy val lambda = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda"))
  .settings(
    name := "feral-lambda",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "io.circe" %%% "circe-core" % circeVersion
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-scalajs" % circeVersion
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
      "io.circe" %%% "circe-jawn" % circeVersion,
    )
  )
