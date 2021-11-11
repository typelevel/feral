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

name := "feral"

ThisBuild / baseVersion := "0.1"

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / crossScalaVersions := Seq("3.1.0", "2.12.15", "2.13.7")

ThisBuild / developers := List(
  Developer("armanbilge", "Arman Bilge", "@armanbilge", url("https://github.com/armanbilge")),
  Developer("bpholt", "Brian Holt", "@bpholt", url("https://github.com/bpholt")),
  Developer("djspiewak", "Daniel Spiewak", "@djspiewak", url("https://github.com/djspiewak"))
)

replaceCommandAlias(
  "ci",
  "; project /; headerCheckAll; scalafmtCheckAll; scalafmtSbtCheck; clean; testIfRelevant; mimaReportBinaryIssuesIfRelevant"
)

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v2"),
    name = Some("Setup NodeJS v14 LTS"),
    params = Map("node-version" -> "14"),
    cond = Some("matrix.ci == 'ciJS'")
  )

val catsEffectVersion = "3.2.9"
val circeVersion = "0.14.1"
val fs2Version = "3.2.2"
val http4sVersion = "0.23.6"

lazy val root =
  project
    .in(file("."))
    .aggregate(
      core.js,
      core.jvm,
      lambdaCloudFormationCustomResource.js,
      lambdaCloudFormationCustomResource.jvm,
      lambda.js,
      lambda.jvm,
      lambdaEvents.js,
      lambdaEvents.jvm,
      lambdaApiGatewayProxyHttp4s.js,
      lambdaApiGatewayProxyHttp4s.jvm
    )
    .enablePlugins(NoPublishPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "feral-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion
    )
  )

lazy val lambda = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda"))
  .settings(
    name := "feral-lambda",
    libraryDependencies ++= Seq(
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
      "co.fs2" %%% "fs2-io" % fs2Version,
      "io.circe" %%% "circe-fs2" % "0.14.0"
    )
  )
  .dependsOn(core)

lazy val lambdaEvents = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-events"))
  .settings(
    name := "feral-lambda-events",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % circeVersion
    )
  )

lazy val lambdaApiGatewayProxyHttp4s = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-api-gateway-proxy-http4s"))
  .settings(
    name := "feral-lambda-api-gateway-proxy-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-core" % http4sVersion
    )
  )
  .dependsOn(lambda, lambdaEvents)

lazy val lambdaCloudFormationCustomResource = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-cloudformation-custom-resource"))
  .settings(
    name := "feral-lambda-cloudformation-custom-resource",
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Ywarn-macros:after")
      case _ => Nil
    }),
    libraryDependencies ++= Seq(
      "io.monix" %%% "newtypes-core" % "0.0.1",
      "io.circe" %%% "circe-generic" % circeVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion
    )
  )
  .dependsOn(lambda)
